package io.split.android.client.service.rules;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.split.android.client.storage.rbs.RuleBasedSegmentStorageImplTest.createRuleBasedSegment;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import io.split.android.client.api.EventMetadata;
import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;

public class RuleBasedSegmentInPlaceUpdateTaskTest {

    private RuleBasedSegmentInPlaceUpdateTask mTask;
    private RuleBasedSegmentStorage mRuleBasedSegmentStorage;
    private RuleBasedSegmentChangeProcessor mChangeProcessor;
    private ISplitEventsManager mEventsManager;

    @Before
    public void setUp() {
        mChangeProcessor = mock(RuleBasedSegmentChangeProcessor.class);
        mRuleBasedSegmentStorage = mock(RuleBasedSegmentStorage.class);
        mEventsManager = mock(ISplitEventsManager.class);
    }

    @Test
    public void splitEventsManagerIsNotifiedWithUpdateEventAndMetadata() {
        RuleBasedSegment ruleBasedSegment = createRuleBasedSegment("segment1");
        long changeNumber = 123L;

        when(mChangeProcessor.process(ruleBasedSegment, changeNumber)).thenReturn(new ProcessedRuleBasedSegmentChange(Set.of(ruleBasedSegment), Collections.emptySet(), 123L, System.currentTimeMillis()));
        when(mRuleBasedSegmentStorage.update(Set.of(ruleBasedSegment), Set.of(), changeNumber, null)).thenReturn(true);

        mTask = getTask(ruleBasedSegment, changeNumber);

        mTask.execute();

        // Verify event is fired with SEGMENT_UPDATE metadata containing segment name and change number
        verify(mEventsManager).notifyInternalEvent(eq(SplitInternalEvent.RULE_BASED_SEGMENTS_UPDATED), argThat(metadata -> {
            if (metadata == null) return false;
            if (metadata.getType() != EventMetadata.Type.SEGMENT_UPDATE) return false;
            if (!metadata.getValues().contains("segment1")) return false;
            if (metadata.getValue() == null || metadata.getValue() != 123L) return false;
            return true;
        }));
    }

    @Test
    public void splitEventsManagerIsNotNotifiedWhenUpdateResultIsFalse() {
        RuleBasedSegment ruleBasedSegment = createRuleBasedSegment("segment1");
        long changeNumber = 123L;

        when(mChangeProcessor.process(ruleBasedSegment, changeNumber)).thenReturn(new ProcessedRuleBasedSegmentChange(Set.of(ruleBasedSegment), Collections.emptySet(), 123L, System.currentTimeMillis()));
        when(mRuleBasedSegmentStorage.update(Set.of(ruleBasedSegment), Set.of(), changeNumber, null)).thenReturn(false);

        mTask = getTask(ruleBasedSegment, changeNumber);

        mTask.execute();

        verify(mEventsManager, never()).notifyInternalEvent(eq(SplitInternalEvent.RULE_BASED_SEGMENTS_UPDATED), any());
    }

    @Test
    public void changeProcessorIsCalledWithRuleBasedSegmentAndChangeNumber() {
        RuleBasedSegment ruleBasedSegment = createRuleBasedSegment("segment1");
        long changeNumber = 123L;

        mTask = getTask(ruleBasedSegment, changeNumber);

        mTask.execute();

        verify(mChangeProcessor).process(ruleBasedSegment, changeNumber);
    }

    @Test
    public void updateIsCalledOnStorage() {
        RuleBasedSegment ruleBasedSegment = createRuleBasedSegment("segment1");
        long changeNumber = 123L;

        when(mChangeProcessor.process(ruleBasedSegment, changeNumber)).thenReturn(new ProcessedRuleBasedSegmentChange(Set.of(ruleBasedSegment), Collections.emptySet(), 123L, System.currentTimeMillis()));

        mTask = getTask(ruleBasedSegment, changeNumber);

        mTask.execute();

        verify(mRuleBasedSegmentStorage).update(Set.of(ruleBasedSegment), Set.of(), changeNumber, null);
    }

    @NonNull
    private RuleBasedSegmentInPlaceUpdateTask getTask(RuleBasedSegment ruleBasedSegment, long changeNumber) {
        return new RuleBasedSegmentInPlaceUpdateTask(mRuleBasedSegmentStorage,
                mChangeProcessor, mEventsManager, ruleBasedSegment, changeNumber);
    }
}
