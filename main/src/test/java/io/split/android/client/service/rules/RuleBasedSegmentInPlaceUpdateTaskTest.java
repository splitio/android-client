package io.split.android.client.service.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.split.android.client.storage.rbs.RuleBasedSegmentStorageImplTest.createRuleBasedSegment;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SdkUpdateMetadata;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.events.metadata.TypedTaskConverter;
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
    public void splitEventsManagerIsNotifiedWithUpdateEvent() {
        RuleBasedSegment ruleBasedSegment = createRuleBasedSegment("segment1");
        long changeNumber = 123L;

        when(mChangeProcessor.process(ruleBasedSegment, changeNumber)).thenReturn(new ProcessedRuleBasedSegmentChange(Set.of(ruleBasedSegment), Collections.emptySet(), 123L, System.currentTimeMillis()));
        when(mRuleBasedSegmentStorage.update(Set.of(ruleBasedSegment), Set.of(), changeNumber, null)).thenReturn(true);

        mTask = getTask(ruleBasedSegment, changeNumber);

        mTask.execute();

        verify(mEventsManager).notifyInternalEvent(eq(SplitInternalEvent.RULE_BASED_SEGMENTS_UPDATED), any());
    }

    @Test
    public void splitEventsManagerIsNotNotifiedWhenUpdateResultIsFalse() {
        RuleBasedSegment ruleBasedSegment = createRuleBasedSegment("segment1");
        long changeNumber = 123L;

        when(mChangeProcessor.process(ruleBasedSegment, changeNumber)).thenReturn(new ProcessedRuleBasedSegmentChange(Set.of(ruleBasedSegment), Collections.emptySet(), 123L, System.currentTimeMillis()));
        when(mRuleBasedSegmentStorage.update(Set.of(ruleBasedSegment), Set.of(), changeNumber, null)).thenReturn(false);

        mTask = getTask(ruleBasedSegment, changeNumber);

        mTask.execute();

        verify(mEventsManager, times(0)).notifyInternalEvent(eq(SplitInternalEvent.RULE_BASED_SEGMENTS_UPDATED), any());
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

    @Test
    public void segmentsUpdatedIncludesMetadataWithActiveAndArchivedSegmentNames() {
        RuleBasedSegment activeSegment = createRuleBasedSegment("active_segment");
        RuleBasedSegment archivedSegment = createRuleBasedSegment("archived_segment");
        long changeNumber = 123L;

        when(mChangeProcessor.process(activeSegment, changeNumber)).thenReturn(
                new ProcessedRuleBasedSegmentChange(Set.of(activeSegment), Set.of(archivedSegment), changeNumber, System.currentTimeMillis()));
        when(mRuleBasedSegmentStorage.update(Set.of(activeSegment), Set.of(archivedSegment), changeNumber, null)).thenReturn(true);

        mTask = getTask(activeSegment, changeNumber);
        mTask.execute();

        verify(mEventsManager).notifyInternalEvent(eq(SplitInternalEvent.RULE_BASED_SEGMENTS_UPDATED), argThat(metadata -> {
            if (metadata == null) return false;
            SdkUpdateMetadata typedMeta = TypedTaskConverter.convertForSdkUpdate(metadata);
            List<String> names = typedMeta.getNames();
            assertNotNull(names);
            assertEquals(2, names.size());
            assertTrue(names.contains("active_segment"));
            assertTrue(names.contains("archived_segment"));
            assertEquals(SdkUpdateMetadata.Type.SEGMENTS_UPDATE, typedMeta.getType());
            return true;
        }));
    }

    @NonNull
    private RuleBasedSegmentInPlaceUpdateTask getTask(RuleBasedSegment ruleBasedSegment, long changeNumber) {
        return new RuleBasedSegmentInPlaceUpdateTask(mRuleBasedSegmentStorage,
                mChangeProcessor, mEventsManager, ruleBasedSegment, changeNumber);
    }
}
