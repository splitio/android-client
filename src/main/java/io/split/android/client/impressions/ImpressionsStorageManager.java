package io.split.android.client.impressions;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.split.android.client.dtos.ChunkHeader;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.storage.IStorage;
import io.split.android.client.track.TrackStorageManager;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

/**
 * Created by guillermo on 1/18/18.
 */

public class ImpressionsStorageManager implements LifecycleObserver {

    private static final String LEGACY_IMPRESSIONS_FILE_NAME = "SPLITIO.impressions";
    private static final String IMPRESSIONS_FILE_PREFIX = "SPLITIO.impressions";
    private static final String IMPRESSIONS_CHUNK_FILE_PREFIX = IMPRESSIONS_FILE_PREFIX + "_#";
    private static final String CHUNK_HEADERS_FILE_NAME = IMPRESSIONS_FILE_PREFIX + "_chunk_headers.json";
    private static final String IMPRESSIONS_FILE_NAME = IMPRESSIONS_CHUNK_FILE_PREFIX + "%d.json";
    private static final int MAX_BYTES_PER_CHUNK = 3000000; //3MB
    private static final int ESTIMATED_IMPRESSION_SIZE = 500; //50 bytes

    private final static Type IMPRESSIONS_FILE_TYPE = new TypeToken<Map<String, List<KeyImpression>>>() {
    }.getType();

    private final static Type LEGACY_IMPRESSIONS_FILE_TYPE = new TypeToken<Map<String, StoredImpressions>>() {
    }.getType();

    private final static String RECORD_KEY_SEPARATOR = "_";
    private final static String RECORD_KEY_FORMAT = "%s" + RECORD_KEY_SEPARATOR + "%s";

    private IStorage mFileStorageManager;
    Map<String, StoredImpressions> mImpressionsToSend;
    ImpressionsStorageManagerConfig mConfig;

    public ImpressionsStorageManager(IStorage storage, ImpressionsStorageManagerConfig config) {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        mFileStorageManager = storage;
        mImpressionsToSend = Collections.synchronizedMap(new HashMap<>());
        mConfig = config;
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
        if (mFileStorageManager.exists(CHUNK_HEADERS_FILE_NAME)) {
            loadImpressionsFromMultipleFiles();
        } else {
            loadImpressionsFromOneFile();
        }
    }

    private void loadImpressionsFromMultipleFiles() {

        try {
            String headerContent = mFileStorageManager.read(CHUNK_HEADERS_FILE_NAME);
            if (headerContent != null) {
                List<ChunkHeader> headers = Json.fromJson(headerContent, ChunkHeader.CHUNK_HEADER_TYPE);
                for (ChunkHeader header : headers) {
                    StoredImpressions storedImpressions = StoredImpressions.from(header.getId(), header.getAttempt(), header.getTimestamp());
                    mImpressionsToSend.put(storedImpressions.id(), storedImpressions);
                    if (!isChunkOutdated(storedImpressions)) {
                        mImpressionsToSend.put(storedImpressions.id(), storedImpressions);
                    }
                }
            }
        } catch (IOException e) {
            Logger.e(e, "Unable to track chunks headers information from disk: " + e.getLocalizedMessage());
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse saved track chunks headers: " + syntaxException.getLocalizedMessage());
        }

        List<Map<String, List<KeyImpression>>> impressions = new ArrayList<>();

        List<String> allFileNames = mFileStorageManager.getAllIds(IMPRESSIONS_CHUNK_FILE_PREFIX);
        for (String fileName : allFileNames) {
            try {
                String file = mFileStorageManager.read(fileName);
                Map<String, List<KeyImpression>> impressionsFile = Json.fromJson(file, IMPRESSIONS_FILE_TYPE);
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
                    if(testImpressionsIndex == -1) {
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
            } catch (IOException e) {
                Logger.e(e, "Unable to impressions file from disk: " + e.getLocalizedMessage());
            } catch (JsonSyntaxException syntaxException) {
                Logger.e(syntaxException, "Unable to parse saved impression: " + syntaxException.getLocalizedMessage());
            }
        }
    }

    private void loadImpressionsFromOneFile() {

        try {
            String storedImpressions = mFileStorageManager.read(LEGACY_IMPRESSIONS_FILE_NAME);
            if (Strings.isNullOrEmpty(storedImpressions)) {
                return;
            }

            Map<String, StoredImpressions> impressions = Json.fromJson(storedImpressions, LEGACY_IMPRESSIONS_FILE_TYPE);
            for (Map.Entry<String, StoredImpressions> entry : impressions.entrySet()) {
                if (!isChunkOutdated(entry.getValue())) {
                    mImpressionsToSend.put(entry.getKey(), entry.getValue());
                }
            }

        } catch (IOException e) {
            Logger.e(e, "Unable to load impressions from disk: " + e.getLocalizedMessage());
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse saved impressions: " + syntaxException.getLocalizedMessage());
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private void saveToDisk() {
        List<ChunkHeader> headers = getChunkHeaders(mImpressionsToSend);

        try {
            String json = Json.toJson(headers);
            mFileStorageManager.write(CHUNK_HEADERS_FILE_NAME, json);
        } catch (IOException e) {
            Logger.e(e, "Could not save tracks headers");
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse tracks to save");
        }

        List<Map<String, List<KeyImpression>>> impressionsChunks = splitChunks(getStoredImpressions());
        int i = 0;
        for (Map<String, List<KeyImpression>> chunk : impressionsChunks) {
            try {
                String json = Json.toJson(chunk);
                String fileName = String.format(IMPRESSIONS_FILE_NAME, i);
                mFileStorageManager.write(fileName, json);
                i++;
            } catch (IOException e) {
                Logger.e(e, "Could not save impressions");
            } catch (JsonSyntaxException syntaxException) {
                Logger.e(syntaxException, "Unable to parse impressions to save");
            }
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

    private List<Map<String, List<KeyImpression>>> splitChunks(List<StoredImpressions> storedImpressions) {

        List<Map<String, List<KeyImpression>>> splitImpressions = new ArrayList<>();
        long bytesCount = 0;
        List<KeyImpression> currentImpressions = new ArrayList<>();
        Map<String, List<KeyImpression>> currentChunk = new HashMap<>();
        for (StoredImpressions storedImpression : storedImpressions) {
            List<TestImpressions> testImpressions = storedImpression.impressions();
            for (TestImpressions testImpression : testImpressions) {
                List<KeyImpression> keyImpressions = testImpression.keyImpressions;
                for (KeyImpression keyImpression : keyImpressions) {
                    if (bytesCount + ESTIMATED_IMPRESSION_SIZE > MAX_BYTES_PER_CHUNK) {
                        currentChunk.put(buildImpressionRecordKey(storedImpression.id(), testImpression.testName), currentImpressions);
                        splitImpressions.add(currentChunk);
                        currentChunk = new HashMap<>();
                        currentImpressions = new ArrayList<>();
                        bytesCount = 0;
                    }
                    currentImpressions.add(keyImpression);
                    bytesCount += ESTIMATED_IMPRESSION_SIZE;
                }
                if (currentImpressions.size() > 0) {
                    currentChunk.put(buildImpressionRecordKey(storedImpression.id(), testImpression.testName), currentImpressions);
                    currentImpressions = new ArrayList<>();
                }
            }
        }
        splitImpressions.add(currentChunk);
        return splitImpressions;
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
}
