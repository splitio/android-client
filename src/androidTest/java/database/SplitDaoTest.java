package database;

import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import helper.FileHelper;
import helper.IntegrationHelper;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.storage.db.SplitDao;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class SplitDaoTest {

    SplitRoomDatabase mRoomDb;
    Context mContext;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = SplitRoomDatabase.getDatabase(mContext, "encripted_api_key");
        mRoomDb.clearAllTables();
    }

    @Test
    public void insertRetrieve() throws InterruptedException {
        long timestamp = System.currentTimeMillis();
        mRoomDb.splitDao().insert(generateData(1, 100, timestamp));
        mRoomDb.splitDao().insert(generateData(101, 200, timestamp));
        mRoomDb.splitDao().insert(generateData(201, 300, timestamp));

        Map<String, SplitEntity> splits = convertToMap(mRoomDb.splitDao().getAll());

        Assert.assertEquals(300, splits.size());
        Assert.assertEquals(timestamp + 1, splits.get("split_1").getTimestamp());
        Assert.assertEquals(timestamp + 150, splits.get("split_150").getTimestamp());
        Assert.assertEquals(timestamp + 300, splits.get("split_300").getTimestamp());
    }

    @Test
    public void jsonIntegrity() {
        long timestamp = System.currentTimeMillis();
        mRoomDb.splitDao().insert(generateData(1, 1, timestamp));

        SplitEntity splitEntity = mRoomDb.splitDao().getAll().get(0);
        Split split = Json.fromJson(splitEntity.getBody(), Split.class);
        Assert.assertEquals("split_1", split.name);
        Assert.assertEquals("split_1", splitEntity.getName());
        Assert.assertEquals(timestamp + 1, splitEntity.getTimestamp());
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
            split.setTimestamp(timestamp + i);
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
