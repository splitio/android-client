package io.split.android.engine.impressions;

import com.google.gson.reflect.TypeToken;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.ChunkHeader;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.impressions.ImpressionsStorageManager;
import io.split.android.client.impressions.ImpressionsStorageManagerConfig;
import io.split.android.client.impressions.StoredImpressions;
import io.split.android.client.storage.IStorage;
import io.split.android.client.storage.MemoryStorage;
import io.split.android.client.utils.Json;

public class ImpressionsStorageTest {

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
    final int MAX_FILE_SIZE = 1000000;

    @Before
    public void setUp() {


        final String FILE_NAME = "SPLITIO.impressions";

        Map<String, StoredImpressions> storedImpressions = new HashMap<>();
        IStorage memStorage = new MemoryStorage();
        final int CHUNK_COUNT = 4;
        for (int i = 0; i < CHUNK_COUNT; i++) {
            String chunkId = String.format("chunk-%d", i);
            List<TestImpressions> testImpressions = new ArrayList<>();
            for (int j = 0; j < 4; j++) {

                String featureName = String.format("feature-%d-%d", i, j);
                List<KeyImpression> impressions = new ArrayList<>();
                for (int k = 0; k < 4; k++) {
                    KeyImpression impression = new KeyImpression();
                    impression.feature = featureName;
                    impression.keyName = String.format("name-%d-%d-%d", i, j, k);
                    impression.treatment = String.format("treatment-%d-%d-%d", i, j, k);
                    impressions.add(impression);
                }
                TestImpressions testImpressionsDTO = new TestImpressions();
                testImpressionsDTO.testName = featureName;
                testImpressionsDTO.keyImpressions = impressions;
                testImpressions.add(testImpressionsDTO);
            }

            long timestamp = System.currentTimeMillis();
            if (i == CHUNK_COUNT - 1) {
                timestamp = timestamp - OUTDATED_LIMIT * 2; // Two day millis, chunck is deprecated and should not be loaded
            }
            StoredImpressions storedImpressionsItem = StoredImpressions.from(chunkId, testImpressions, timestamp);
            storedImpressions.put(chunkId, storedImpressionsItem);
        }
        try {
            String allImpressionsJson = Json.toJson(storedImpressions);
            memStorage.write(FILE_NAME, allImpressionsJson);
        } catch (IOException e) {
        }
        ImpressionsStorageManagerConfig config = new ImpressionsStorageManagerConfig();
        config.setImpressionsMaxSentAttempts(MAX_ATTEMPTS);
        config.setImpressionsChunkOudatedTime(OUTDATED_LIMIT);
        mImpStorage = new ImpressionsStorageManager(memStorage, config);
    }

    @Test
    public void getStoredImpressions() {

        List<StoredImpressions> storedImpressions = mImpStorage.getStoredImpressions();

        Assert.assertEquals(3, storedImpressions.size());
        Assert.assertNotEquals(-1, getIndexForStoredImpression("chunk-0", storedImpressions));
        Assert.assertNotEquals(-1, getIndexForStoredImpression("chunk-1", storedImpressions));
        Assert.assertNotEquals(-1, getIndexForStoredImpression("chunk-2", storedImpressions));

        StoredImpressions storedImpression = storedImpressions.get(getIndexForStoredImpression("chunk-0", storedImpressions));
        Assert.assertEquals(4, storedImpression.impressions().size());
        List<TestImpressions> testImpressions = storedImpression.impressions();
        Assert.assertEquals(4, testImpressions.size());
        Assert.assertEquals("feature-0-0", testImpressions.get(0).testName);
        Assert.assertEquals(4, testImpressions.get(0).keyImpressions.size());
        Assert.assertEquals("feature-0-3", testImpressions.get(3).testName);
    }

    @Test
    public void storeImpressions() {
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

        Assert.assertEquals(4, storedImpressions.size());
        StoredImpressions storedImpression = storedImpressions.get(getIndexForUUIDStoredImpression(storedImpressions));
        List<TestImpressions> testImpressions = storedImpression.impressions();
        Assert.assertEquals(2, testImpressions.size());

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

        List<StoredImpressions> storedImpressions = mImpStorage.getStoredImpressions();

        StoredImpressions chunk1 = storedImpressions.get(getIndexForStoredImpression("chunk-1", storedImpressions));
        mImpStorage.succeededStoredImpression(chunk1);
        storedImpressions = mImpStorage.getStoredImpressions();

        Assert.assertEquals(2, storedImpressions.size());
        Assert.assertEquals(-1, getIndexForStoredImpression("chunk-1", storedImpressions));
        Assert.assertNotEquals(-1, getIndexForStoredImpression("chunk-0", storedImpressions));
        Assert.assertNotEquals(-1, getIndexForStoredImpression("chunk-2", storedImpressions));
    }

    @Test
    public void impressionsSentFailure() {

        List<StoredImpressions> storedImpressions = mImpStorage.getStoredImpressions();
        StoredImpressions chunk1 = storedImpressions.get(getIndexForStoredImpression("chunk-1", storedImpressions));
        mImpStorage.failedStoredImpression(chunk1);
        storedImpressions = mImpStorage.getStoredImpressions();

        int chunk1Index = getIndexForStoredImpression("chunk-1", storedImpressions);
        Assert.assertTrue(chunk1Index > -1);

        chunk1 = storedImpressions.get(chunk1Index);

        Assert.assertEquals(3, storedImpressions.size());
        Assert.assertEquals(1, chunk1.getAttempts());
    }

    @Test
    public void impressionsSendMaxAttemptsReached() {
        List<StoredImpressions> storedImpressions = mImpStorage.getStoredImpressions();
        StoredImpressions chunk1 = storedImpressions.get(getIndexForStoredImpression("chunk-1", storedImpressions));
        for (int i = 0; i <= MAX_ATTEMPTS; i++) {
            mImpStorage.failedStoredImpression(chunk1);
        }
        storedImpressions = mImpStorage.getStoredImpressions();
        Assert.assertEquals(-1, getIndexForStoredImpression("chunk-1", storedImpressions));
    }


    @Test
    public void testSaveAndLoadChunkFiles() throws IOException {

        IStorage memStorage = new MemoryStorage();

        ImpressionsStorageManagerConfig config = new ImpressionsStorageManagerConfig();
        config.setImpressionsChunkOudatedTime(3600 * 1000);
        ImpressionsStorageManager savingManager = new ImpressionsStorageManager(memStorage, config);

        int chunkCount = 5;
        int[] testData = {1, 500, 1000, 1500, 2000, 2500, 3000, 4000, 5000, 6500};

        int totalImpressionsCount = getSum(testData);
        int totalImpressionsSize = sizeInBytes(totalImpressionsCount);
        int filesCount = (chunkCount * totalImpressionsSize / MAX_FILE_SIZE) + 1;

        List<StoredImpressions> storedImpressionsList = new ArrayList<>();
        for (int c = 0; c < chunkCount; c++) {
            List<KeyImpression> keyImpressions = new ArrayList<>();
            for (int i = 0; i < testData.length; i++) {
                int impressionsCount = testData[i];
                String testName = String.format("FEATURE_%d", i);
                for (int j = 0; j < impressionsCount; j++) {
                    KeyImpression keyImpression = new KeyImpression();
                    keyImpression.bucketingKey = null;
                    keyImpression.keyName = String.format("CUSTOMER_ID");
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

        ImpressionsStorageManager loadingManager = new ImpressionsStorageManager(memStorage, config);
        List<StoredImpressions> loadedChunks = loadingManager.getStoredImpressions();

        String headerContent = memStorage.read(CHUNK_HEADERS_FILE_NAME);
        List<ChunkHeader> headers = Json.fromJson(headerContent, chunkHeaderType);
        List<String> allImpressionsFiles = memStorage.getAllIds("SPLITIO.impressions_#");
        List<Map<String, List<KeyImpression>>> loadedKeyImpressions = new ArrayList<>();
        List<Integer> sizes = new ArrayList<>();
        for (int i = 0; i < filesCount; i++) {
            String file = memStorage.read(String.format(IMPRESSIONS_FILE_NAME, i));
            Map<String, List<KeyImpression>> impressionsFile = Json.fromJson(file, impressionsFileType);
            loadedKeyImpressions.add(impressionsFile);
            sizes.add(sizeInBytes(impressionsFile.size()));
        }

        Assert.assertNotNull(headerContent);
        Assert.assertEquals(chunkCount, headers.size());
        Assert.assertEquals(filesCount, allImpressionsFiles.size());
        for (int i = 0; i < filesCount; i++) {
            Assert.assertTrue(sizes.get(i).intValue() <= MAX_FILE_SIZE);
        }

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

        IStorage memStorage = new MemoryStorage();

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
                    keyImpression.keyName = String.format("CUSTOMER_ID");
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
    public void testMissingImpressionsFile() throws IOException {
        IStorage memStorage = new MemoryStorage();
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

    // Helpers
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
        for (int i = 0; i < values.length; i++) {
            sum += values[i];
        }
        return sum;
    }
}
