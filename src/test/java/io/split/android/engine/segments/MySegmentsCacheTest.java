package io.split.android.engine.segments;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.cache.IMySegmentsCache;
import io.split.android.client.cache.MySegmentsCache;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.storage.IStorage;
import io.split.android.client.storage.MemoryStorage;
import io.split.android.client.utils.Json;

public class MySegmentsCacheTest {

    IMySegmentsCache mCache = null;

    @Before
    public void setupUp(){

        final String FILE_NAME = "SPLITIO.mysegments";


        Map<String, List<MySegment>> allSegments = new HashMap<String, List<MySegment>>();
        IStorage memStorage = new MemoryStorage();

        for(int i = 0; i < 3; i++) {
            String key = String.format("key-%d", i);
            List<MySegment> segments  = new ArrayList<>();
            for(int j = 0; j < 4; j++) {
                MySegment segment = new MySegment();
                segment.id = String.format("id-%d-%d", i, j);
                segment.name = String.format("name-%d-%d", i, j);
                segments.add(segment);
            }
            allSegments.put(key, segments);
        }
        try {
            String allSegmentsJson = Json.toJson(allSegments);
            memStorage.write(FILE_NAME, allSegmentsJson);
        } catch (IOException e) {
        }
        mCache = new MySegmentsCache(memStorage);
    }

    @Test
    public void getMySegments() {

        List<MySegment> segments0 = mCache.getMySegments("key-0");
        List<MySegment> segments1 = mCache.getMySegments("key-1");
        List<MySegment> segments2 = mCache.getMySegments("key-2");

        Assert.assertEquals(4, segments0.size());
        Assert.assertEquals("id-0-0", segments0.get(0).id);
        Assert.assertEquals("id-0-2", segments0.get(2).id);

        Assert.assertEquals(4, segments1.size());
        Assert.assertEquals("id-1-1", segments1.get(1).id);
        Assert.assertEquals("id-1-2", segments1.get(2).id);

        Assert.assertEquals(4, segments2.size());
        Assert.assertEquals("id-2-0", segments2.get(0).id);
        Assert.assertEquals("id-2-2", segments2.get(2).id);
    }

    @Test
    public void setMySegments() {
        String key = "key-0";
        List<MySegment> segments = mCache.getMySegments(key);
        MySegment segment = new MySegment();
        segment.id = "id-test-1";
        segment.name = "test-1";
        segments.add(segment);
        mCache.setMySegments(key, segments);

        List<MySegment> segmentsTest = mCache.getMySegments(key);
        Assert.assertEquals(5, segmentsTest.size());
        Assert.assertEquals("id-test-1", segmentsTest.get(4).id);
    }

    @Test
    public void deleteMySegments() {
        String key1 = "key-1";
        String key2 = "key-2";
        mCache.deleteMySegments(key1);
        mCache.deleteMySegments(key2);

        List<MySegment> segments1 = mCache.getMySegments(key1);
        List<MySegment> segments2 = mCache.getMySegments(key2);

        Assert.assertNull(segments1);
        Assert.assertNull(segments2);
    }

}
