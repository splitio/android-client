package io.split.android.client.service.impressions.unique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UniqueKeysTrackerImplTest {

    private UniqueKeysTrackerImpl mUniqueKeysTracker;

    @Before
    public void setUp() {
        mUniqueKeysTracker = new UniqueKeysTrackerImpl();
    }

    @Test
    public void nullUserKeyReturnsFalseWhenTracking() {
        assertFalse(mUniqueKeysTracker.track(null, "split_1"));
    }

    @Test
    public void nullFeatureNameReturnsFalseWhenTracking() {
        assertFalse(mUniqueKeysTracker.track("key", null));
    }

    @Test
    public void trackingWorksCorrectly() throws InterruptedException {
        Map<String, Set<String>> expectedMap = new HashMap<>();
        Set<String> key1Set = new HashSet<>();
        key1Set.add("split_1");
        key1Set.add("split_2");
        key1Set.add("split_3");
        expectedMap.put("key1", key1Set);

        Set<String> key2Set = new HashSet<>();
        key2Set.add("split_2");
        key2Set.add("split_3");
        key2Set.add("split_4");
        expectedMap.put("key2", key2Set);

        Thread threadKey1 = new Thread(() -> {
            mUniqueKeysTracker.track("key1", "split_1");
            mUniqueKeysTracker.track("key1", "split_1");
            mUniqueKeysTracker.track("key1", "split_2");
            mUniqueKeysTracker.track("key1", "split_3");
            mUniqueKeysTracker.track("key2", "split_4");
        });

        Thread threadKey2 = new Thread(() -> {
            mUniqueKeysTracker.track("key2", "split_2");
            mUniqueKeysTracker.track("key2", "split_3");
        });

        threadKey1.start();
        threadKey2.start();
        threadKey1.join();
        threadKey2.join();

        Map<String, Set<String>> stringSetMap = mUniqueKeysTracker.popAll();
        assertEquals(expectedMap, stringSetMap);
    }

    @Test
    public void popAllClearsTrackedValues() {
        mUniqueKeysTracker.track("key1", "split_1");
        mUniqueKeysTracker.track("key1", "split_2");

        Map<String, Set<String>> expectedMap = new HashMap<>();
        Set<String> key1Set = new HashSet<>();
        key1Set.add("split_1");
        key1Set.add("split_2");
        expectedMap.put("key1", key1Set);

        Map<String, Set<String>> keys = mUniqueKeysTracker.popAll();
        assertEquals(expectedMap, keys);

        assertTrue(mUniqueKeysTracker.popAll().isEmpty());
    }
}
