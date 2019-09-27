package io.split.android.client.impressions;

import android.annotation.SuppressLint;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.dtos.ChunkHeader;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.storage.FileStorageHelper;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

/**
 * Created by guillermo on 1/18/18.
 */

public class ImpressionsStorageManager {

    private static final String LEGACY_IMPRESSIONS_FILE_NAME = "SPLITIO.impressions";
    private static final String IMPRESSIONS_FILE_PREFIX = "SPLITIO.impressions";
    private static final String IMPRESSIONS_CHUNK_FILE_PREFIX = IMPRESSIONS_FILE_PREFIX + "_#";
    private static final String CHUNK_HEADERS_FILE_NAME = IMPRESSIONS_FILE_PREFIX + "_chunk_headers.json";
    private static final String IMPRESSIONS_FILE_NAME = IMPRESSIONS_CHUNK_FILE_PREFIX + "%d.json";

    private final static Type IMPRESSIONS_FILE_TYPE = new TypeToken<Map<String, List<KeyImpression>>>() {
    }.getType();

    private final static Type LEGACY_IMPRESSIONS_FILE_TYPE = new TypeToken<Map<String, StoredImpressions>>() {
    }.getType();

    private final static String RECORD_KEY_SEPARATOR = "_";
    private final static String RECORD_KEY_FORMAT = "%s" + RECORD_KEY_SEPARATOR + "%s";

    private IImpressionsStorage mFileStorageManager;
    private FileStorageHelper mFileStorageHelper;
    Map<String, StoredImpressions> mImpressionsToSend;
    ImpressionsStorageManagerConfig mConfig;

    public ImpressionsStorageManager(IImpressionsStorage storage, ImpressionsStorageManagerConfig config) {
        this(storage, config, new FileStorageHelper());
    }

    public ImpressionsStorageManager(IImpressionsStorage storage, ImpressionsStorageManagerConfig config, FileStorageHelper fileStorageHelper) {
        mFileStorageManager = storage;
        mImpressionsToSend = new ConcurrentHashMap<String, StoredImpressions>();
        mConfig = config;
        mFileStorageHelper = fileStorageHelper;
        loadImpressionsFromDisk();
    }

    @SuppressLint("DefaultLocale")
    public void storeImpressions(List<KeyImpression> impressions) throws IOException {
        if (impressions == null || impressions.isEmpty()) {
            return; // Nothing to write
        }

        Map<String, List<KeyImpression>> tests = new HashMap<>();

        for (KeyImpression ki : impressions) {
            List<KeyImpression> impressionsForTest = tests.get(ki.feature);
            if (impressionsForTest == null) {
                impressionsForTest = new ArrayList<>();
                tests.put(ki.feature, impressionsForTest);
            }
            impressionsForTest.add(ki);
        }

        List<TestImpressions> toShip = Lists.newArrayList();

        for (Map.Entry<String, List<KeyImpression>> entry : tests.entrySet()) {
            String testName = entry.getKey();
            List<KeyImpression> keyImpressions = entry.getValue();

            TestImpressions testImpressionsDTO = new TestImpressions();
            testImpressionsDTO.testName = testName;
            testImpressionsDTO.keyImpressions = keyImpressions;

            toShip.add(testImpressionsDTO);
        }

        String chunkId = UUID.randomUUID().toString();
        mImpressionsToSend.put(chunkId, StoredImpressions.from(chunkId, toShip, System.currentTimeMillis()));
    }

    public List<StoredImpressions> getStoredImpressions() {
        return new ArrayList<>(mImpressionsToSend.values());
    }

    public void failedStoredImpression(StoredImpressions storedImpression) {
        if (chunkCanBeStored(storedImpression)) {
            chunkFailed(storedImpression.id());
        }
    }

    public void succeededStoredImpression(StoredImpressions storedImpression) {
        if (chunkCanBeStored(storedImpression)) {
            chunkSucceeded(storedImpression.id());
        }
    }

    public void saveToDisk() {
        mFileStorageManager.write(mImpressionsToSend);
    }

    public void close() {
        saveToDisk();
    }

    private boolean chunkCanBeStored(StoredImpressions storedImpressions) {
        if (storedImpressions == null) {
            return false;
        }
        if (storedImpressions.id() == null || storedImpressions.id().isEmpty()) {
            return false;
        }
        if (storedImpressions.impressions() == null || storedImpressions.impressions().isEmpty()) {
            return false;
        }
        return true;
    }

    private String readStringChunk(String chunkId) {
        try {
            return mFileStorageManager.read(chunkId);
        } catch (IOException e) {
            Logger.e(e, "Could not read chunk %s", chunkId);
        }
        return null;
    }

    private void chunkSucceeded(String chunkId) {
        mImpressionsToSend.remove(chunkId);
    }

    @SuppressLint("DefaultLocale")
    private void chunkFailed(String chunkId) {

        if (Strings.isNullOrEmpty(chunkId)) {
            return;
        }

        StoredImpressions failedChunk = mImpressionsToSend.get(chunkId);
        if (failedChunk.getAttempts() >= mConfig.getImpressionsMaxSentAttempts() || isChunkOutdated(failedChunk)) {
            mImpressionsToSend.remove(chunkId);
        } else {
            failedChunk.addAttempt();
        }
    }

    private boolean isChunkOutdated(StoredImpressions chunk) {
        long diff = System.currentTimeMillis() - chunk.getTimestamp();

        if (diff > mConfig.getImpressionsChunkOudatedTime()) {
            return true;
        }
        return false;
    }

    private void loadImpressionsFromDisk() {
        if(mFileStorageManager.exists(LEGACY_IMPRESSIONS_FILE_NAME)) {
            loadEventsFromLegacyFile();
            mFileStorageManager.delete(LEGACY_IMPRESSIONS_FILE_NAME);
        } else if(mFileStorageManager.exists(CHUNK_HEADERS_FILE_NAME)) {
            loadEventsFromChunkFiles();
            deleteOldChunksFiles();
        } else {
            loadEventsFilesByLine();
        }
    }

    private void loadEventsFilesByLine() {
        Map<String, StoredImpressions> loaded = mFileStorageManager.read();
        if (loaded != null) {
            mImpressionsToSend.putAll(loaded);
        }
    }

    private void loadEventsFromLegacyFile() {
        String storedImpressions = mFileStorageHelper.checkMemoryAndReadFile(LEGACY_IMPRESSIONS_FILE_NAME, mFileStorageManager);
        if (!Strings.isNullOrEmpty(storedImpressions)) {
            Map<String, StoredImpressions> impressions = Json.fromJson(storedImpressions, LEGACY_IMPRESSIONS_FILE_TYPE);
            for (Map.Entry<String, StoredImpressions> entry : impressions.entrySet()) {
                if (!isChunkOutdated(entry.getValue())) {
                    mImpressionsToSend.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private void loadEventsFromChunkFiles() {
        createChunksFromHeaders(mFileStorageHelper.readAndParseChunkHeadersFile(CHUNK_HEADERS_FILE_NAME, mFileStorageManager));
        createImpressionsFromChunkFiles();
        removeChunksWithoutImpressions();
    }

    private void createChunksFromHeaders(List<ChunkHeader> headers) {
        if(headers != null) {
            for (ChunkHeader header : headers) {
                StoredImpressions storedImpressions = StoredImpressions.from(header.getId(), header.getAttempt(), header.getTimestamp());
                mImpressionsToSend.put(storedImpressions.id(), storedImpressions);
                if (!isChunkOutdated(storedImpressions)) {
                    mImpressionsToSend.put(storedImpressions.id(), storedImpressions);
                }
            }
        }
    }

    private void removeChunksWithoutImpressions() {
        List<String> chunkIds = new ArrayList(mImpressionsToSend.keySet());
        for(String chunkId : chunkIds) {
            StoredImpressions chunk = mImpressionsToSend.get(chunkId);
            if(chunk != null && chunk.impressions() != null && chunk.impressions().size() == 0) {
                mImpressionsToSend.remove(chunkId);
            }
        }
    }

    private void createImpressionsFromChunkFiles() {
        List<Map<String, List<KeyImpression>>> impressions = new ArrayList<>();
        List<String> allFileNames = mFileStorageManager.getAllIds(IMPRESSIONS_CHUNK_FILE_PREFIX);
        for (String fileName : allFileNames) {
            String fileContent = mFileStorageHelper.checkMemoryAndReadFile(fileName, mFileStorageManager);
            if(fileContent != null) {
                parseImpressions(fileContent);
            }
        }
    }

    private Map<String, List<KeyImpression>> parseImpressionChunkFileContent(String json) {
        Map<String, List<KeyImpression>> impressionsFile = null;
        try {
            impressionsFile = Json.fromJson(json, IMPRESSIONS_FILE_TYPE);
        } catch (JsonSyntaxException e) {
            Logger.e("Error parsing impressions file");
        }
        return impressionsFile;
    }

    private void parseImpressions(String json) {
        Map<String, List<KeyImpression>> impressionsFile = parseImpressionChunkFileContent(json);

        if(impressionsFile == null) {
            return;
        }

        for (Map.Entry<String, List<KeyImpression>> impressionsChunk : impressionsFile.entrySet()) {
            String storedImpressionId = getStoredImpressionsIdFromRecordKey(impressionsChunk.getKey());
            String testName = getTestNameFromRecordKey(impressionsChunk.getKey());

            StoredImpressions storedImpressions = mImpressionsToSend.get(storedImpressionId);
            if (storedImpressions == null) {
                storedImpressions = StoredImpressions.from(storedImpressionId, 0, System.currentTimeMillis());
            }
            List<TestImpressions> testImpressionsList = storedImpressions.impressions();
            if (testImpressionsList == null) {
                testImpressionsList = new ArrayList();
            }

            int testImpressionsIndex = getImpressionsIndexForTest(testName, testImpressionsList);
            TestImpressions testImpressions;
            if (testImpressionsIndex == -1) {
                testImpressions = new TestImpressions();
                testImpressions.testName = testName;
                testImpressions.keyImpressions = new ArrayList<>();
            } else {
                testImpressions = testImpressionsList.get(testImpressionsIndex);
                storedImpressions.impressions().remove(testImpressionsIndex);
            }

            testImpressions.keyImpressions.addAll(impressionsChunk.getValue());
            storedImpressions.impressions().add(testImpressions);
        }
    }


    private List<ChunkHeader> getChunkHeaders(Map<String, StoredImpressions> storedImpressions) {
        List<ChunkHeader> chunkHeaders = new ArrayList<>();
        for (StoredImpressions storedImpression : storedImpressions.values()) {
            ChunkHeader header = new ChunkHeader(storedImpression.id(), storedImpression.getAttempts(), storedImpression.getTimestamp());
            chunkHeaders.add(header);
        }
        return chunkHeaders;
    }

    private String buildImpressionRecordKey(String storedImpressionId, String testName) {
        return String.format(RECORD_KEY_FORMAT, storedImpressionId, testName);
    }

    private String getStoredImpressionsIdFromRecordKey(String recordKey) {

        String chunkId = "";
        try {
            chunkId = recordKey.substring(0, recordKey.indexOf(RECORD_KEY_SEPARATOR));
        } catch (IndexOutOfBoundsException e) {
            Logger.e("Record key not valid loading impressions from disk: " + e.getLocalizedMessage());
        }
        return chunkId;
    }

    private String getTestNameFromRecordKey(String recordKey) {
        String testName = "";
        try {
            testName = recordKey.substring(recordKey.indexOf(RECORD_KEY_SEPARATOR) + 1, recordKey.length());
        } catch (IndexOutOfBoundsException e) {
            Logger.e("Record key not valid loading impressions from disk: " + e.getLocalizedMessage());
        }
        return testName;
    }

    private int getImpressionsIndexForTest(String testName, List<TestImpressions> impressionsList) {
        int indexFound = -1;
        int i = 0;
        while (indexFound == -1 && impressionsList.size() > i) {
            TestImpressions currentImpressions = impressionsList.get(i);
            if(testName.equals(currentImpressions.testName)) {
                indexFound = i;
            }
            i++;
        }
        return indexFound;
    }

    private void deleteOldChunksFiles() {
        List<String> oldChunkFiles = mFileStorageManager.getAllIds(IMPRESSIONS_CHUNK_FILE_PREFIX);
        for(String fileName : oldChunkFiles) {
            mFileStorageManager.delete(fileName);
        }
        mFileStorageManager.delete(CHUNK_HEADERS_FILE_NAME);
    }
}
