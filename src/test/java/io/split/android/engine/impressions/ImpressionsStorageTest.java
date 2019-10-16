package io.split.android.engine.impressions;

import com.google.gson.reflect.TypeToken;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.dtos.ChunkHeader;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.impressions.IImpressionsStorage;
import io.split.android.client.impressions.ImpressionsFileStorage;
import io.split.android.client.impressions.ImpressionsStorageManager;
import io.split.android.client.impressions.ImpressionsStorageManagerConfig;
import io.split.android.client.impressions.StoredImpressions;
import io.split.android.client.utils.Json;
import io.split.android.fake.ImpressionsFileStorageStub;

public class ImpressionsStorageTest {

    IImpressionsStorage mStorage;
    ImpressionsStorageManager mImpStorage = null;
    final long OUTDATED_LIMIT = 3600 * 1000; // One day millis
    final int MAX_ATTEMPTS = 3;
    private static final int ESTIMATED_IMPRESSION_SIZE = 500;
    private static final String LEGACY_IMPRESSIONS_FILE_NAME = "SPLITIO.impressions";

    Type chunkHeaderType = new TypeToken<List<ChunkHeader>>() {
    }.getType();
    Type impressionsFileType = new TypeToken<Map<String, List<KeyImpression>>>() {
    }.getType();
    final String CHUNK_HEADERS_FILE_NAME = "SPLITIO.impressions_chunk_headers.json";
    final String IMPRESSIONS_FILE_NAME = "SPLITIO.impressions_#%d.json";

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Before
    public void setUp()throws IOException {

        File rootFolder = new File("./build");
        File folder = new File(rootFolder, "test_folder");
        if(folder.exists()) {
            for(File file : folder.listFiles()){
                file.delete();
            }
            folder.delete();
        }

        ImpressionsStorageManagerConfig config = new ImpressionsStorageManagerConfig();
        config.setImpressionsMaxSentAttempts(MAX_ATTEMPTS);
        config.setImpressionsChunkOudatedTime(OUTDATED_LIMIT);
        mStorage = new ImpressionsFileStorage(rootFolder, "test_folder");
        mImpStorage = new ImpressionsStorageManager(mStorage, config);
    }

    @Test
    public void storeImpressions() {
        int chunkCount = 1;
        populateManager(chunkCount, mImpStorage);
        List<StoredImpressions> existingImpressions = mImpStorage.getStoredImpressions();
        String existingChunkId = existingImpressions.get(0).id();

        List<KeyImpression> impressions = new ArrayList<>();
        for (int j = 0; j < 2; j++) {
            String featureName = String.format("feature-test-%d", j);
            for (int k = 0; k < 4; k++) {
                KeyImpression impression = new KeyImpression();
                impression.feature = featureName;
                impression.keyName = String.format("name-%d-%d", j, k);
                impression.treatment = String.format("treatment-%d-%d", j, k);
                impressions.add(impression);
            }
        }
        try {
            mImpStorage.storeImpressions(impressions);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<StoredImpressions> storedImpressions = mImpStorage.getStoredImpressions();
        String newChunk = null;
        for(StoredImpressions imp : storedImpressions) {
            if(!imp.id().equals(existingChunkId)) {
                newChunk = imp.id();
            }
        }
        int newChunkIndex = getIndexForStoredImpression(newChunk, storedImpressions);
        Assert.assertEquals(chunkCount + 1, storedImpressions.size());
        StoredImpressions storedImpression = storedImpressions.get(newChunkIndex);
        List<TestImpressions> testImpressions = storedImpression.impressions();

        Set<String> testNames = new HashSet<>();
        testNames.add(testImpressions.get(0).testName);
        testNames.add(testImpressions.get(1).testName);
        Assert.assertTrue(testNames.contains("feature-test-0"));
        Assert.assertTrue(testNames.contains("feature-test-1"));
        Assert.assertEquals(4, testImpressions.get(0).keyImpressions.size());
        Assert.assertEquals(4, testImpressions.get(1).keyImpressions.size());
    }

    @Test
    public void impressionsSentSuccess() {

        int chunkCount = 2;
        populateManager(chunkCount, mImpStorage);
        List<StoredImpressions> storedImpressions = mImpStorage.getStoredImpressions();

        StoredImpressions chunkSent = storedImpressions.get(0);
        String chunkIdSent = chunkSent.id();
        mImpStorage.succeededStoredImpression(chunkSent);
        storedImpressions = mImpStorage.getStoredImpressions();

        Assert.assertEquals(chunkCount - 1, storedImpressions.size());
        Assert.assertEquals(-1, getIndexForStoredImpression(chunkIdSent, storedImpressions));
        for(int i = 0; i< storedImpressions.size(); i++) {
            String curId = storedImpressions.get(i).id();
            if(!chunkIdSent.equals(curId)) {
                Assert.assertNotEquals(-1, getIndexForStoredImpression(curId, storedImpressions));
            }
        }
    }

    @Test
    public void impressionsSentFailure() {

        int chunkCount = 3;
        populateManager(chunkCount, mImpStorage);
        List<StoredImpressions> storedImpressions = mImpStorage.getStoredImpressions();

        StoredImpressions chunkSent = storedImpressions.get(0);
        String chunkIdSent = chunkSent.id();
        mImpStorage.failedStoredImpression(chunkSent);
        storedImpressions = mImpStorage.getStoredImpressions();

        Assert.assertEquals(chunkCount, storedImpressions.size());
        Assert.assertEquals(1, chunkSent.getAttempts());
    }

    @Test
    public void impressionsSendMaxAttemptsReached() {
        int chunkCount = 3;
        populateManager(chunkCount, mImpStorage);
        List<StoredImpressions> storedImpressions = mImpStorage.getStoredImpressions();
        StoredImpressions chunk1 = storedImpressions.get(0);
        String chunkId = chunk1.id();
        for (int i = 0; i <= MAX_ATTEMPTS; i++) {
            mImpStorage.failedStoredImpression(chunk1);
        }
        storedImpressions = mImpStorage.getStoredImpressions();
        Assert.assertEquals(-1, getIndexForStoredImpression(chunkId, storedImpressions));
    }

    @Test
    public void testSaveAndLoadChunkFiles() throws IOException {


        //IImpressionsStorage memStorage = new ImpressionsFileStorageStub();

        ImpressionsStorageManagerConfig config = new ImpressionsStorageManagerConfig();
        config.setImpressionsChunkOudatedTime(3600 * 1000);
        ImpressionsStorageManager savingManager = new ImpressionsStorageManager(mStorage, config);

        int chunkCount = 5;
        //int[] testData = {1, 500, 1000, 1500, 2000, 2500, 3000, 4000, 5000, 6500};
        int[] testData = {1, 5, 10, 1, 20, 1, 30, 40, 50, 65};

        int totalImpressionsCount = getSum(testData);
        int totalImpressionsSize = sizeInBytes(totalImpressionsCount);

        List<StoredImpressions> storedImpressionsList = new ArrayList<>();
        for (int c = 0; c < chunkCount; c++) {
            List<KeyImpression> keyImpressions = new ArrayList<>();
            for (int i = 0; i < testData.length; i++) {
                int impressionsCount = testData[i];
                String testName = String.format("FEATURE_%d", i);
                for (int j = 0; j < impressionsCount; j++) {
                    KeyImpression keyImpression = new KeyImpression();
                    keyImpression.bucketingKey = null;
                    keyImpression.keyName = "CUSTOMER_ID";
                    keyImpression.feature = testName;
                    keyImpression.time = System.currentTimeMillis();
                    keyImpression.treatment = "off";
                    keyImpressions.add(keyImpression);
                }
            }
            savingManager.storeImpressions(keyImpressions);
        }

        List<StoredImpressions> savedChunks = savingManager.getStoredImpressions();

        savingManager.close(); // Close saves to disk

        ImpressionsStorageManager loadingManager = new ImpressionsStorageManager(mStorage, config);
        List<StoredImpressions> loadedChunks = loadingManager.getStoredImpressions();

        Assert.assertEquals(chunkCount, loadedChunks.size());
        Assert.assertEquals(savedChunks.size(), loadedChunks.size());
        for (StoredImpressions savedChunk : savedChunks) {
            StoredImpressions loadedChunk = loadedChunks.get(getIndexForStoredImpression(savedChunk.id(), loadedChunks));
            Assert.assertEquals(savedChunk.getAttempts(), loadedChunk.getAttempts());
            Assert.assertEquals(savedChunk.impressions().size(), loadedChunk.impressions().size());
            List<TestImpressions> savedTestImpressions = savedChunk.impressions();
            List<TestImpressions> loadedTestImpressions = loadedChunk.impressions();

            for (TestImpressions savedTestImp : savedTestImpressions) {
                int index = getImpressionsIndexForTest(savedTestImp.testName, loadedTestImpressions);
                Assert.assertEquals(savedTestImp.keyImpressions.size(), loadedTestImpressions.get(index).keyImpressions.size());
            }
        }
    }

    @Test
    public void testLoadLegacyFromLegacyFile() throws IOException {

        IImpressionsStorage memStorage = mStorage;

        int chunkCount = 5;
        int[] testData = {1, 500, 1000, 1500, 2000, 2500, 3000, 4000, 5000, 6500};

        Map<String, StoredImpressions> storedImpressionsList = new HashMap<>();

        for (int c = 0; c < chunkCount; c++) {
            List<TestImpressions> testImpressionsList = new ArrayList<>();
            for (int i = 0; i < testData.length; i++) {
                int impressionsCount = testData[i];
                String testName = String.format("FEATURE_%d", i);
                List<KeyImpression> keyImpressions = new ArrayList<>();
                for (int j = 0; j < impressionsCount; j++) {
                    KeyImpression keyImpression = new KeyImpression();
                    keyImpression.bucketingKey = null;
                    keyImpression.keyName = "CUSTOMER_ID";
                    keyImpression.feature = testName;
                    keyImpressions.add(keyImpression);
                }
                TestImpressions testImpressions = new TestImpressions();
                testImpressions.testName = testName;
                testImpressions.keyImpressions = keyImpressions;
                testImpressionsList.add(testImpressions);

            }
            String chunkId = "chunk_" + c;
            StoredImpressions storedImpressions = StoredImpressions.from(chunkId, testImpressionsList, System.currentTimeMillis());
            storedImpressionsList.put(chunkId, storedImpressions);
        }

        String jsonChunks = Json.toJson(storedImpressionsList);
        memStorage.write(LEGACY_IMPRESSIONS_FILE_NAME, jsonChunks);

        ImpressionsStorageManagerConfig config = new ImpressionsStorageManagerConfig();
        config.setImpressionsChunkOudatedTime(3600 * 1000);
        ImpressionsStorageManager loadingManager = new ImpressionsStorageManager(memStorage, config);
        List<StoredImpressions> loadedChunks = loadingManager.getStoredImpressions();

        Assert.assertEquals(chunkCount, loadedChunks.size());

        for (int i = 0; i < chunkCount; i++) {
            StoredImpressions loadedChunk = loadedChunks.get(getIndexForStoredImpression("chunk_" + i, loadedChunks));
            Assert.assertEquals(0, loadedChunk.getAttempts());
            Assert.assertEquals(testData.length, loadedChunk.impressions().size());
            List<TestImpressions> loadedTestImpressions = loadedChunk.impressions();

            for (int j = 0; j < testData.length; j++) {
                int index = getImpressionsIndexForTest(String.format("FEATURE_%d", j), loadedTestImpressions);
                Assert.assertEquals(testData[j], loadedTestImpressions.get(index).keyImpressions.size());
            }
        }
    }

    @Test
    public void testLoadLegacyFromLegacyChunkFiles() throws IOException {

        IImpressionsStorage memStorage = mStorage;

        int chunkCount = 5;
        int testCount = 4;
        int impCount = 15;

        int lastFileNumber = 0;
        int fileNumber = 0;
        List<ChunkHeader> headers = new ArrayList<>();
        Map<String, List<KeyImpression>> impressionsList = new HashMap<>();
        for (int c = 0; c < chunkCount; c++) {
            ChunkHeader header = new ChunkHeader("id-" + c, 0, 111111);
            headers.add(header);
            String testName = null;
            List<KeyImpression> keyImpressions = new ArrayList<>();
            for (int i = 0; i < testCount; i++) {
                testName = String.format("FEATURE_%d", i);
                for (int j = 0; j < impCount; j++) {
                    KeyImpression keyImpression = new KeyImpression();
                    keyImpression.bucketingKey = null;
                    keyImpression.keyName = "CUSTOMER_ID";
                    keyImpression.feature = testName;
                    keyImpressions.add(keyImpression);
                    if((j + 6) % 5 == 0) {
                        fileNumber++;
                    }
                    if(fileNumber != lastFileNumber) {
                        impressionsList.put(header.getId() + "_" + testName, keyImpressions);
                        String fileName = String.format(IMPRESSIONS_FILE_NAME, lastFileNumber);
                        memStorage.write(fileName, Json.toJson(impressionsList));
                        keyImpressions = new ArrayList<>();
                        impressionsList = new HashMap<>();
                        lastFileNumber = fileNumber;
                    }
                }
            }
            impressionsList.put(header.getId() + "_" + testName, keyImpressions);

        }
        memStorage.write(CHUNK_HEADERS_FILE_NAME, Json.toJson(headers));
        String fileName = String.format(IMPRESSIONS_FILE_NAME, lastFileNumber);
        memStorage.write(fileName, Json.toJson(impressionsList));

        ImpressionsStorageManagerConfig config = new ImpressionsStorageManagerConfig();
        config.setImpressionsChunkOudatedTime(3600 * 1000);
        ImpressionsStorageManager loadingManager = new ImpressionsStorageManager(memStorage, config);
        List<StoredImpressions> loadedChunks = loadingManager.getStoredImpressions();

        Assert.assertEquals(chunkCount, loadedChunks.size());

        for (int i = 0; i < chunkCount; i++) {
            StoredImpressions loadedChunk = loadedChunks.get(getIndexForStoredImpression("id-" + i, loadedChunks));
            Assert.assertEquals(0, loadedChunk.getAttempts());
            List<TestImpressions> loadedTestImpressions = loadedChunk.impressions();

            for (int j = 0; j < testCount; j++) {
                int index = getImpressionsIndexForTest(String.format("FEATURE_%d", j), loadedTestImpressions);
                Assert.assertEquals(impCount, loadedTestImpressions.get(index).keyImpressions.size());
            }
        }
    }

    @Test
    public void testMissingImpressionsFile() throws IOException {
        IImpressionsStorage memStorage = new ImpressionsFileStorageStub();
        List<ChunkHeader> headers = new ArrayList<>();
        for(int i = 0; i < 3; i++) {
            ChunkHeader c = new ChunkHeader("c" + i, 0);
            headers.add(c);
        }
        String json = Json.toJson(headers);
        memStorage.write(CHUNK_HEADERS_FILE_NAME, json);

        ImpressionsStorageManager manager = new ImpressionsStorageManager(memStorage, new ImpressionsStorageManagerConfig());

        Assert.assertNotNull(manager);

    }

    @Test
    public void loadEmptyJsonLFile() throws IOException {
        ImpressionsStorageManagerConfig config = new ImpressionsStorageManagerConfig();
        config.setImpressionsMaxSentAttempts(MAX_ATTEMPTS);
        config.setImpressionsChunkOudatedTime(OUTDATED_LIMIT);
        mStorage.write("SPLITIO.impressions_chunk_id_1.jsonl", "");
        ImpressionsStorageManager manager = new ImpressionsStorageManager(mStorage, config);
        List<StoredImpressions> imp = manager.getStoredImpressions();

        Assert.assertNotNull(imp);
        Assert.assertEquals(0, imp.size());


    }

    // Helpers
    private void populateManager(int chunks, ImpressionsStorageManager manager) {
        for (int i = 0; i < chunks; i++) {
            String featureName = String.format("feature-%d", i);
            List<KeyImpression> impressions = new ArrayList<>();
            for (int k = 0; k < 4; k++) {
                KeyImpression impression = new KeyImpression();
                impression.feature = featureName;
                impression.keyName = String.format("name-%d-%d", i, k);
                impression.treatment = String.format("treatment-%d-%d", i, k);
                impressions.add(impression);
            }
            try {
                manager.storeImpressions(impressions);
            } catch (IOException e) {
                // All test should fail
            }
        }
    }

    private int getIndexForStoredImpression(String chunkId, List<StoredImpressions> impressions) {
        int index = -1;
        int i = 0;
        boolean found = false;

        while (!found && impressions.size() > i) {
            String id = impressions.get(i).id();
            if (chunkId.equals(id)) {
                found = true;
                index = i;
            }
            i++;
        }
        return index;
    }

    private int getIndexForUUIDStoredImpression(List<StoredImpressions> impressions) {
        int index = -1;
        int i = 0;
        boolean found = false;

        while (!found && impressions.size() > i) {
            String id = impressions.get(i).id();
            if (!id.contains("chunk")) {
                found = true;
                index = i;
            }
            i++;
        }
        return index;
    }

    private int sizeInBytes(int length) {
        return ESTIMATED_IMPRESSION_SIZE * length;
    }

    private int getImpressionsIndexForTest(String testName, List<TestImpressions> impressionsList) {
        int indexFound = -1;
        int i = 0;
        while (indexFound == -1 && impressionsList.size() > i) {
            TestImpressions currentImpressions = impressionsList.get(i);
            if (testName.equals(currentImpressions.testName)) {
                indexFound = i;
            }
            i++;
        }
        return indexFound;
    }

    private int getSum(int[] values) {
        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        return sum;
    }
}
