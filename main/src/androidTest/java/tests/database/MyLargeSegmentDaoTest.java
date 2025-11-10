package tests.database;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.split.android.client.storage.db.MyLargeSegmentEntity;

public class MyLargeSegmentDaoTest extends GenericDaoTest {

    @Test
    public void insertRetrieve() {
        long timestamp = System.currentTimeMillis();
        mRoomDb.myLargeSegmentDao().update(generateData("key1", 1, 100, timestamp));
        mRoomDb.myLargeSegmentDao().update(generateData("key2",101, 200, timestamp));
        mRoomDb.myLargeSegmentDao().update(generateData("key3",201, 300, timestamp));

        MyLargeSegmentEntity msKey1 = mRoomDb.myLargeSegmentDao().getByUserKey("key1");
        MyLargeSegmentEntity msKey2 = mRoomDb.myLargeSegmentDao().getByUserKey("key2");
        MyLargeSegmentEntity msKey3 = mRoomDb.myLargeSegmentDao().getByUserKey("key3");

        Assert.assertEquals("key1", msKey1.getUserKey());
        Assert.assertEquals("key2", msKey2.getUserKey());
        Assert.assertEquals("key3", msKey3.getUserKey());

        Assert.assertEquals(timestamp + 1, msKey1.getUpdatedAt());
        Assert.assertEquals(timestamp + 101, msKey2.getUpdatedAt());
        Assert.assertEquals(timestamp + 201, msKey3.getUpdatedAt());
    }

    @Test
    public void insertUpdateRetrieve() {
        long timestamp = System.currentTimeMillis();
        mRoomDb.myLargeSegmentDao().update(generateData("key1", 1, 10, timestamp));
        mRoomDb.myLargeSegmentDao().update(generateData("key1", 500, 505, timestamp));

        MyLargeSegmentEntity msKey1 = mRoomDb.myLargeSegmentDao().getByUserKey("key1");
        Set<String> segments = new HashSet<String>(Arrays.asList(msKey1.getSegmentList().split(",")));

        Assert.assertEquals("key1", msKey1.getUserKey());
        Assert.assertEquals(timestamp + 500, msKey1.getUpdatedAt());
        Assert.assertFalse(segments.contains("segment1"));
        Assert.assertTrue(segments.contains("segment500"));
        Assert.assertTrue(segments.contains("segment505"));
    }

    @Test
    public void segmentsIntegrity() {
        long timestamp = System.currentTimeMillis();
        mRoomDb.myLargeSegmentDao().update(generateData("key1", 1, 10, timestamp));

        MyLargeSegmentEntity mySegmentEntity = mRoomDb.myLargeSegmentDao().getByUserKey("key1");
        Set<String> segments = new HashSet<String>(Arrays.asList(mySegmentEntity.getSegmentList().split(",")));

        Assert.assertEquals("key1", mySegmentEntity.getUserKey());
        Assert.assertEquals(timestamp + 1, mySegmentEntity.getUpdatedAt());
        Assert.assertEquals(10, segments.size());
        Assert.assertTrue(segments.contains("segment1"));
        Assert.assertTrue(segments.contains("segment5"));
        Assert.assertTrue(segments.contains("segment10"));
    }

    private MyLargeSegmentEntity generateData(String key, int from, int to, long timestamp) {
        MyLargeSegmentEntity segmentEntity = new MyLargeSegmentEntity();
        List<String> mySegmentList = new ArrayList<>();
        for(int i = from; i<=to; i++) {
            mySegmentList.add("segment" + i);
        }
        segmentEntity.setUserKey(key);
        segmentEntity.setSegmentList(String.join(",", mySegmentList));
        segmentEntity.setUpdatedAt(timestamp + from);
        return segmentEntity;
    }
}
