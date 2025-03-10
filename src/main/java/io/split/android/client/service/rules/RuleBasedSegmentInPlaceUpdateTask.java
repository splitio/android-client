package io.split.android.client.service.rules;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.client.utils.logger.Logger;

public class RuleBasedSegmentInPlaceUpdateTask implements SplitTask {

    private final RuleBasedSegmentStorage mRuleBasedSegmentStorage;
    private final long mChangeNumber;
    private final RuleBasedSegment mRuleBasedSegment;
    private final RuleBasedSegmentChangeProcessor mChangeProcessor;
    private final ISplitEventsManager mEventsManager;

    public RuleBasedSegmentInPlaceUpdateTask(@NonNull RuleBasedSegmentStorage ruleBasedSegmentStorage,
                                             @NonNull RuleBasedSegmentChangeProcessor changeProcessor,
                                             @NonNull ISplitEventsManager eventsManager,
                                             @NonNull RuleBasedSegment ruleBasedSegment,
                                             long changeNumber) {
        mRuleBasedSegmentStorage = checkNotNull(ruleBasedSegmentStorage);
        mRuleBasedSegment = checkNotNull(ruleBasedSegment);
        mChangeProcessor = checkNotNull(changeProcessor);
        mEventsManager = eventsManager;
        mChangeNumber = changeNumber;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            ProcessedRuleBasedSegmentChange processedChange = mChangeProcessor.process(mRuleBasedSegment, mChangeNumber);
            boolean triggerSdkUpdate = mRuleBasedSegmentStorage.update(processedChange.getActive(), processedChange.getArchived(), mChangeNumber);

            if (triggerSdkUpdate) {
                mEventsManager.notifyInternalEvent(SplitInternalEvent.RULE_BASED_SEGMENTS_UPDATED);
            }

            Logger.v("Updated rule based segment");
            return SplitTaskExecutionInfo.success(SplitTaskType.RULE_BASED_SEGMENT_SYNC);
        } catch (Exception ex) {
            Logger.e("Could not update rule based segment");

            return SplitTaskExecutionInfo.error(SplitTaskType.RULE_BASED_SEGMENT_SYNC);
        }
    }
}
