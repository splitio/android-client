package io.split.android.engine.traffictypes;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.cache.InMemoryTrafficTypesCache;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;

public class TrafficTypesCacheTests {

    private InMemoryTrafficTypesCache cache;

    @Before
    public void setUp(){
        List<Split> splits = new ArrayList<>();
        splits.add(newSplit("s0", "trafficType0", Status.ACTIVE));
        splits.add(newSplit("s1", "trafficType1", Status.ACTIVE));
        splits.add(newSplit("s2", "trafficType2", Status.ACTIVE));
        splits.add(newSplit("s3", "trafficType3", Status.ACTIVE));
        cache = new InMemoryTrafficTypesCache();
        cache.updateFromSplits(splits);
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
        InMemoryTrafficTypesCache cache = new InMemoryTrafficTypesCache();
        cache.updateFromSplits(null);
        Assert.assertFalse(cache.contains("trafficType0"));
        Assert.assertFalse(cache.contains("trafficType1"));
        Assert.assertFalse(cache.contains("trafficType2"));
        Assert.assertFalse(cache.contains("trafficType3"));
    }

    @Test
    public void initWithNullSplitsAndUpdate() {
        InMemoryTrafficTypesCache cache = new InMemoryTrafficTypesCache();
        cache.updateFromSplits(null);
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
        InMemoryTrafficTypesCache cache = new InMemoryTrafficTypesCache();
        Assert.assertFalse(cache.contains("trafficType0"));
        Assert.assertFalse(cache.contains("trafficType1"));
        Assert.assertFalse(cache.contains("trafficType2"));
        Assert.assertFalse(cache.contains("trafficType3"));
    }

    @Test
    public void noSetWithSplitsAndUpdate() {
        InMemoryTrafficTypesCache cache = new InMemoryTrafficTypesCache();
        cache.updateFromSplits(null);
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
    public void singleUpdateArchived() {

        List<Split> splits = new ArrayList<>();

        cache.updateFromSplit(newSplit("s0", "trafficType0", Status.ARCHIVED));
        Assert.assertFalse(cache.contains("trafficType0"));
    }

    @Test
    public void singleUpdateActive() {
        cache.updateFromSplit(newSplit("s10", "trafficType10", Status.ACTIVE));
        Assert.assertTrue(cache.contains("trafficType10"));
    }

    @Test
    public void updateWithNullSplit() {
        boolean exceptionOccurs = false;
        try {
            cache.updateFromSplit(null);
        } catch (Exception e) {
            exceptionOccurs = true;
        }
        Assert.assertFalse(exceptionOccurs);
    }

    private Split newSplit(String name, String trafficType, Status status) {
        Split split = new Split();
        split.name = name;
        split.trafficTypeName = trafficType;
        split.status = status;
        return split;
    }
}
