package io.split.android.client.storage.rbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.split.android.client.dtos.Excluded;
import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.dtos.Status;

public class RuleBasedSegmentStorageImplTest {

    private RuleBasedSegmentStorageImpl storage;
    private PersistentRuleBasedSegmentStorage mPersistentStorage;

    @Before
    public void setUp() {
        mPersistentStorage = mock(PersistentRuleBasedSegmentStorage.class);
        storage = new RuleBasedSegmentStorageImpl(mPersistentStorage);
    }

    @Test
    public void get() {
        RuleBasedSegment segment = createRuleBasedSegment("segment1");
        storage.update(Set.of(segment), null, 1);
        assertNotNull(storage.get("segment1"));
    }

    @Test
    public void sequentialUpdate() {
        RuleBasedSegment segment1 = createRuleBasedSegment("segment1");
        RuleBasedSegment segment2 = createRuleBasedSegment("segment2");

        Set<RuleBasedSegment> toAdd = new HashSet<>();
        toAdd.add(segment1);
        toAdd.add(segment2);

        storage.update(toAdd, null, 2);
        assertNotNull(storage.get("segment1"));
        assertNotNull(storage.get("segment2"));
        assertEquals(2, storage.getChangeNumber());

        Set<RuleBasedSegment> toRemove = new HashSet<>();
        toRemove.add(segment1);
        storage.update(null, toRemove, 3);
        assertNull(storage.get("segment1"));
        assertNotNull(storage.get("segment2"));
        assertEquals(3, storage.getChangeNumber());
    }

    @Test
    public void defaultChangeNumberIsMinusOne() {
        assertEquals(-1, storage.getChangeNumber());
    }

    @Test
    public void updateChangeNumber() {
        storage.update(null, null, 5);
        assertEquals(5, storage.getChangeNumber());
    }

    @Test
    public void contains() {
        RuleBasedSegment segment = createRuleBasedSegment("segment1");
        Set<RuleBasedSegment> segmentNames = Set.of(segment);
        storage.update(segmentNames, null, 1);

        Set<String> segmentNames2 = new HashSet<>();
        segmentNames2.add("segment1");
        segmentNames2.add("segment2");

        assertTrue(storage.contains(Set.of("segment1")));
        assertFalse(storage.contains(segmentNames2));
    }

    @Test
    public void clearRemovesAllSegments() {
        RuleBasedSegment segment = createRuleBasedSegment("segment1");
        storage.update(Set.of(segment), null, 1);
        storage.clear();
        assertNull(storage.get("segment1"));
    }

    @Test
    public void clearResetsChangeNumber() {
        RuleBasedSegment segment = createRuleBasedSegment("segment1");
        storage.update(Set.of(segment), null, 10);
        long preClearChangeNumber = storage.getChangeNumber();

        storage.clear();

        assertEquals(10, preClearChangeNumber);
        assertEquals(-1, storage.getChangeNumber());
    }

    @Test
    public void updateWithNullAddAndRemoveIsSafe() {
        long initialChangeNumber = storage.getChangeNumber();
        storage.update(null, null, 10);
        assertEquals(10, storage.getChangeNumber());
        assertNotEquals(initialChangeNumber, storage.getChangeNumber());
    }

    @Test
    public void segmentRemoval() {
        RuleBasedSegment segment1 = createRuleBasedSegment("segment1");
        RuleBasedSegment segment2 = createRuleBasedSegment("segment2");

        Set<RuleBasedSegment> toAdd = new HashSet<>();
        toAdd.add(segment1);
        toAdd.add(segment2);
        storage.update(toAdd, null, 1);

        assertNotNull(storage.get("segment1"));
        assertNotNull(storage.get("segment2"));

        Set<RuleBasedSegment> toRemove = new HashSet<>();
        toRemove.add(createRuleBasedSegment("segment1"));
        storage.update(null, toRemove, 2);

        assertNull(storage.get("segment1"));
        assertNotNull(storage.get("segment2"));
    }

    @Test
    public void segmentRemovalOfSameSegment() {
        RuleBasedSegment segment1 = createRuleBasedSegment("segment1");
        Set<RuleBasedSegment> segments = Collections.singleton(segment1);

        storage.update(segments, segments, 1);
        assertNull(storage.get("segment1"));
        assertEquals(1, storage.getChangeNumber());
    }

    @Test
    public void updateReturnsTrueWhenThereWereAddedSegments() {
        RuleBasedSegment segment1 = createRuleBasedSegment("segment1");
        RuleBasedSegment segment2 = createRuleBasedSegment("segment2");
        Set<RuleBasedSegment> toAdd = new HashSet<>();
        toAdd.add(segment1);
        toAdd.add(segment2);

        assertTrue(storage.update(toAdd, null, 1));
    }

    @Test
    public void loadLocalGetsSnapshotFromPersistentStorage() {
        when(mPersistentStorage.getSnapshot()).thenReturn(new RuleBasedSegmentSnapshot(Set.of(), 1));

        storage.loadLocal();

        verify(mPersistentStorage).getSnapshot();
    }

    @Test
    public void loadLocalPopulatesValues() {
        RuleBasedSegmentSnapshot snapshot = new RuleBasedSegmentSnapshot(Set.of(createRuleBasedSegment("segment1")),
                1);
        when(mPersistentStorage.getSnapshot()).thenReturn(snapshot);

        long initialCn = storage.getChangeNumber();
        RuleBasedSegment initialSegment1 = storage.get("segment1");

        storage.loadLocal();

        long finalCn = storage.getChangeNumber();
        RuleBasedSegment finalSegment1 = storage.get("segment1");

        assertEquals(-1, initialCn);
        assertEquals(1, finalCn);
        assertNull(initialSegment1);
        assertNotNull(finalSegment1);
    }

    @Test
    public void clearCallsClearOnPersistentStorage() {
        storage.clear();

        verify(mPersistentStorage).clear();
    }

    private static RuleBasedSegment createRuleBasedSegment(String name) {
        return new RuleBasedSegment(name,
                "user",
                1,
                Status.ACTIVE,
                new ArrayList<>(),
                new Excluded());
    }
}
