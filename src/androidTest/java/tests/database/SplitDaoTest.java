package tests.database;

import android.util.Log;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import helper.FileHelper;
import helper.IntegrationHelper;
import io.split.android.client.dtos.Split;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.utils.Json;

public class SplitDaoTest extends GenericDaoTest {

    @Test
    public void insertRetrieve() {
        long timestamp = System.currentTimeMillis();
        mRoomDb.splitDao().insert(generateData(1, 100, timestamp));
        mRoomDb.splitDao().insert(generateData(101, 200, timestamp));
        mRoomDb.splitDao().insert(generateData(201, 300, timestamp));

        Map<String, SplitEntity> splits = convertToMap(mRoomDb.splitDao().getAll());

        Assert.assertEquals(300, splits.size());
        Assert.assertEquals(timestamp + 1, splits.get("split_1").getUpdatedAt());
        Assert.assertEquals(timestamp + 150, splits.get("split_150").getUpdatedAt());
        Assert.assertEquals(timestamp + 300, splits.get("split_300").getUpdatedAt());
    }

    @Test
    public void jsonIntegrity() {
        long timestamp = System.currentTimeMillis();
        mRoomDb.splitDao().insert(generateData(1, 1, timestamp));

        SplitEntity splitEntity = mRoomDb.splitDao().getAll().get(0);
        Split split = Json.fromJson(splitEntity.getBody(), Split.class);
        Assert.assertEquals("split_1", split.name);
        Assert.assertEquals("split_1", splitEntity.getName());
        Assert.assertEquals(timestamp + 1, splitEntity.getUpdatedAt());
    }

    @Test
    public void insertDelete() {
        long timestamp = System.currentTimeMillis();
        mRoomDb.splitDao().insert(generateData(1, 100, timestamp));
        List<SplitEntity> splits = mRoomDb.splitDao().getAll();
        mRoomDb.splitDao().delete(splits.stream().map(split -> split.getName()).collect(Collectors.toList()));
        List<SplitEntity> splitsAfterDelete = mRoomDb.splitDao().getAll();

        Assert.assertEquals(100, splits.size());
        Assert.assertEquals(0, splitsAfterDelete.size());
    }

    @Test
    public void performance10() {
        performance(10);
    }

    @Test
    public void performance100() {
        performance(100);
    }

    @Test
    public void performance1000() {
        performance(1000);
    }

    @Test
    @Ignore("Too resource intensive for CI")
    public void performance10000() {
        performance(10000);
    }

    private void performance(int count) {

        final String TAG = "SplitDaoTest_performance";

        List<SplitEntity> splitEntities = generateData(1, count, 100000);
        long start = System.currentTimeMillis();
        mRoomDb.splitDao().insert(splitEntities);
        long writeTime = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        splitEntities = mRoomDb.splitDao().getAll();
        long readTime = System.currentTimeMillis() - start;

        IntegrationHelper.logSeparator(TAG);
        Log.i(TAG, "-> " +count  + " splits");
        Log.i(TAG, String.format("Write time: %d segs, (%d millis) ", readTime / 100, readTime));
        Log.i(TAG, String.format("Read time: %d segs, (%d millis) ", writeTime / 100, writeTime));
        IntegrationHelper.logSeparator(TAG);

        Assert.assertEquals(count, splitEntities.size());
    }

    private List<SplitEntity> generateData(int from, int to, long timestamp) {
        List<SplitEntity> splitList = new ArrayList<>();
        for(int i = from; i<=to; i++) {
            SplitEntity split = new SplitEntity();
            String splitName = "split_" + i;
            split.setName(splitName);
            split.setBody(loadSplit().replace("feature_xx", splitName));
            split.setUpdatedAt(timestamp + i);
            splitList.add(split);
        }
        return splitList;
    }

    private Map<String, SplitEntity> convertToMap(List<SplitEntity> list) {
        Map<String, SplitEntity> map = list.stream()
                .collect(Collectors.toMap(SplitEntity::getName, split -> split));
        return map;
    }

    private String loadSplit() {
        return new FileHelper().loadFileContent(mContext,"split.json");
    }
}
