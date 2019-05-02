package io.split.android.engine.traffictypes;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.cache.TrafficTypesCache;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;

public class TrafficTypesCacheTests {

    private TrafficTypesCache cache;

    @Before
    public void setUp(){
        List<Split> splits = new ArrayList<>();
        splits.add(newSplit("s0", "trafficType0", Status.ACTIVE));
        splits.add(newSplit("s1", "trafficType1", Status.ACTIVE));
        splits.add(newSplit("s2", "trafficType2", Status.ACTIVE));
        splits.add(newSplit("s3", "trafficType3", Status.ACTIVE));
        cache = new TrafficTypesCache();
        cache.setFromSplits(splits);
    }

    @Test
    public void initialTrafficTypes() {
        Assert.assertTrue(cache.contains("trafficType0"));
        Assert.assertTrue(cache.contains("trafficType1"));
        Assert.assertTrue(cache.contains("trafficType2"));
        Assert.assertTrue(cache.contains("trafficType3"));
    }

    @Test
    public void remove2TrafficTypes() {

        List<Split> splits = new ArrayList<>();
        splits.add(newSplit("s0", "trafficType0", Status.ARCHIVED));
        splits.add(newSplit("s1", "trafficType1", Status.ARCHIVED));

        cache.updateFromSplits(splits);
        Assert.assertFalse(cache.contains("trafficType0"));
        Assert.assertFalse(cache.contains("trafficType1"));
        Assert.assertTrue(cache.contains("trafficType2"));
        Assert.assertTrue(cache.contains("trafficType3"));
    }

    @Test
    public void severalTrafficTypeUpdatesFinalActive() {

        List<Split> splits = new ArrayList<>();
        splits.add(newSplit("s0", "trafficType0", Status.ARCHIVED));
        splits.add(newSplit("s01", "trafficType0", Status.ACTIVE));
        splits.add(newSplit("s01", "trafficType0", Status.ARCHIVED));
        splits.add(newSplit("s02", "trafficType0", Status.ACTIVE));

        cache.updateFromSplits(splits);
        Assert.assertTrue(cache.contains("trafficType0"));
        Assert.assertTrue(cache.contains("trafficType1"));
        Assert.assertTrue(cache.contains("trafficType2"));
        Assert.assertTrue(cache.contains("trafficType3"));
    }

    @Test
    public void severalTrafficTypeUpdatesFinalArchived() {

        List<Split> splits = new ArrayList<>();
        splits.add(newSplit("s0", "trafficType0", Status.ARCHIVED));
        splits.add(newSplit("s01", "trafficType0", Status.ACTIVE));
        splits.add(newSplit("s01", "trafficType0", Status.ARCHIVED));
        splits.add(newSplit("s02", "trafficType0", Status.ACTIVE));
        splits.add(newSplit("s02", "trafficType0", Status.ARCHIVED));

        cache.updateFromSplits(splits);
        Assert.assertFalse(cache.contains("trafficType0"));
        Assert.assertTrue(cache.contains("trafficType1"));
        Assert.assertTrue(cache.contains("trafficType2"));
        Assert.assertTrue(cache.contains("trafficType3"));
    }

    @Test
    public void overflowArchived() {

        List<Split> splits = new ArrayList<>();
        splits.add(newSplit("s0", "trafficType0", Status.ARCHIVED));
        splits.add(newSplit("s01", "trafficType0", Status.ARCHIVED));
        splits.add(newSplit("s01", "trafficType0", Status.ARCHIVED));
        splits.add(newSplit("s02", "trafficType0", Status.ACTIVE));
        splits.add(newSplit("s02", "trafficType0", Status.ARCHIVED));

        cache.updateFromSplits(splits);
        Assert.assertFalse(cache.contains("trafficType0"));
        Assert.assertTrue(cache.contains("trafficType1"));
        Assert.assertTrue(cache.contains("trafficType2"));
        Assert.assertTrue(cache.contains("trafficType3"));
    }

    @Test
    public void initWithNullSplits() {
        TrafficTypesCache cache = new TrafficTypesCache();
        cache.setFromSplits(null);
        Assert.assertFalse(cache.contains("trafficType0"));
        Assert.assertFalse(cache.contains("trafficType1"));
        Assert.assertFalse(cache.contains("trafficType2"));
        Assert.assertFalse(cache.contains("trafficType3"));
    }

    @Test
    public void initWithNullSplitsAndUpdate() {
        TrafficTypesCache cache = new TrafficTypesCache();
        cache.setFromSplits(null);
        List<Split> splits = new ArrayList<>();
        splits.add(newSplit("s0", "trafficType0", Status.ACTIVE));
        splits.add(newSplit("s1", "trafficType1", Status.ACTIVE));

        cache.updateFromSplits(splits);
        Assert.assertTrue(cache.contains("trafficType0"));
        Assert.assertTrue(cache.contains("trafficType1"));
        Assert.assertFalse(cache.contains("trafficType2"));
        Assert.assertFalse(cache.contains("trafficType3"));
    }

    @Test
    public void noSetWithSplits() {
        TrafficTypesCache cache = new TrafficTypesCache();
        Assert.assertFalse(cache.contains("trafficType0"));
        Assert.assertFalse(cache.contains("trafficType1"));
        Assert.assertFalse(cache.contains("trafficType2"));
        Assert.assertFalse(cache.contains("trafficType3"));
    }

    @Test
    public void noSetWithSplitsAndUpdate() {
        TrafficTypesCache cache = new TrafficTypesCache();
        cache.setFromSplits(null);
        List<Split> splits = new ArrayList<>();
        splits.add(newSplit("s0", "trafficType0", Status.ACTIVE));
        splits.add(newSplit("s1", "trafficType1", Status.ACTIVE));

        cache.updateFromSplits(splits);
        Assert.assertTrue(cache.contains("trafficType0"));
        Assert.assertTrue(cache.contains("trafficType1"));
        Assert.assertFalse(cache.contains("trafficType2"));
        Assert.assertFalse(cache.contains("trafficType3"));
    }

    private Split newSplit(String name, String trafficType, Status status) {
        Split split = new Split();
        split.name = name;
        split.trafficTypeName = trafficType;
        split.status = status;
        return split;
    }
}
