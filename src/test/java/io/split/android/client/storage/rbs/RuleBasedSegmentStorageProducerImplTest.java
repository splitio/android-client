package io.split.android.client.storage.rbs;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.split.android.client.storage.rbs.RuleBasedSegmentStorageImplTest.createRuleBasedSegment;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.dtos.RuleBasedSegment;

public class RuleBasedSegmentStorageProducerImplTest {

    private PersistentRuleBasedSegmentStorage mPersistentStorage;
    private RuleBasedSegmentStorageProducerImpl storage;

    @Before
    public void setUp() {
        mPersistentStorage = mock(PersistentRuleBasedSegmentStorage.class);
        storage = new RuleBasedSegmentStorageProducerImpl(mPersistentStorage, new ConcurrentHashMap<>(), new AtomicLong(-1));
    }

    @Test
    public void sequentialUpdate() {
        RuleBasedSegment segment1 = createRuleBasedSegment("segment1");
        RuleBasedSegment segment2 = createRuleBasedSegment("segment2");

        Set<RuleBasedSegment> toAdd = new HashSet<>();
        toAdd.add(segment1);
        toAdd.add(segment2);

        storage.update(toAdd, null, 2);

        verify(mPersistentStorage).update(argThat(argument -> argument.size() == 2), argThat(Set::isEmpty), eq(2L));
    }

    @Test
    public void updateChangeNumber() {
        storage.update(null, null, 5);

        verify(mPersistentStorage).update(argThat(Set::isEmpty), argThat(Set::isEmpty), eq(5L));
    }

    @Test
    public void clear() {
        storage.clear();

        verify(mPersistentStorage).clear();
    }

    @Test
    public void loadLocal() {
        when(mPersistentStorage.getSnapshot()).thenReturn(new RuleBasedSegmentSnapshot(Map.of(), 1));

        storage.loadLocal();

        verify(mPersistentStorage).getSnapshot();
    }
}
