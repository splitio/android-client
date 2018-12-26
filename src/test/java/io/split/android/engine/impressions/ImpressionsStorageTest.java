package io.split.android.engine.impressions;

        import org.junit.Assert;

        import org.junit.Before;
        import org.junit.Test;

        import java.io.IOException;
        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.HashSet;
        import java.util.List;
        import java.util.Map;
        import java.util.Set;

        import io.split.android.client.dtos.KeyImpression;
        import io.split.android.client.dtos.TestImpressions;
        import io.split.android.client.impressions.ImpressionsStorageManager;
        import io.split.android.client.impressions.StoredImpressions;
        import io.split.android.client.storage.IStorage;
        import io.split.android.client.storage.MemoryStorage;
        import io.split.android.client.utils.Json;

public class ImpressionsStorageTest {

    ImpressionsStorageManager mImpStorage = null;

    @Before
    public void setupUp(){

        final String FILE_NAME = "SPLITIO.impressions";


        Map<String, StoredImpressions> storedImpressions = new HashMap<>();
        IStorage memStorage = new MemoryStorage();
        final int CHUNK_COUNT = 4;
        for(int i = 0; i < CHUNK_COUNT; i++) {
            String chunkId = String.format("chunk-%d", i);
            List<TestImpressions> testImpressions  = new ArrayList<>();
            for(int j = 0; j < 4; j++) {

                String featureName = String.format("feature-%d-%d", i, j);
                List<KeyImpression> impressions  = new ArrayList<>();
                for(int k = 0; k < 4; k++) {
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
            if(i == CHUNK_COUNT -1){
                timestamp = timestamp - 3600 * 1000 * 2; // Two day millis, chunck is deprecated and should not be loaded
            }
            StoredImpressions storedImpressionsItem = StoredImpressions.from(chunkId, testImpressions, timestamp);
            storedImpressions.put(chunkId, storedImpressionsItem);
        }
        try {
            String allImpressionsJson = Json.toJson(storedImpressions);
            memStorage.write(FILE_NAME, allImpressionsJson);
        } catch (IOException e) {
        }
        mImpStorage = new ImpressionsStorageManager(memStorage);
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
    public void storeImpressions(){
        List<KeyImpression> impressions  = new ArrayList<>();
        for(int j = 0; j < 2; j++) {
            String featureName = String.format("feature-test-%d", j);
            for(int k = 0; k < 4; k++) {
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


    // Helpers
    private int getIndexForStoredImpression(String chunkId, List<StoredImpressions> impressions) {

        int index = -1;
        int i = 0;
        boolean found = false;

        while(!found && impressions.size() > i){
            String id = impressions.get(i).id();
            if(chunkId.equals(id)){
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

        while(!found && impressions.size() > i){
            String id = impressions.get(i).id();
            if(!id.contains("chunk")){
                found = true;
                index = i;
            }
            i++;
        }
        return index;
    }
}
