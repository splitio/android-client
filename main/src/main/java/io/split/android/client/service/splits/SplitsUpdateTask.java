package io.split.android.client.service.splits;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.List;

import io.split.android.client.api.EventMetadata;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.events.metadata.EventMetadataHelpers;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.synchronizer.SplitsChangeChecker;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.logger.Logger;

public class SplitsUpdateTask implements SplitTask {

    private final SplitsStorage mSplitsStorage;
    @Nullable
    private Long mChangeNumber;
    @Nullable
    private Long mRbsChangeNumber;
    private final SplitsSyncHelper mSplitsSyncHelper;
    private final ISplitEventsManager mEventsManager;
    private SplitsChangeChecker mChangeChecker;
    private final RuleBasedSegmentStorage mRuleBasedSegmentStorage;

    public SplitsUpdateTask(SplitsSyncHelper splitsSyncHelper,
                            SplitsStorage splitsStorage,
                            RuleBasedSegmentStorage ruleBasedSegmentStorage,
                            @Nullable Long since,
                            @Nullable Long rbsSince,
                            @NonNull ISplitEventsManager eventsManager) {
        mSplitsStorage = checkNotNull(splitsStorage);
        mRuleBasedSegmentStorage = checkNotNull(ruleBasedSegmentStorage);
        mSplitsSyncHelper = checkNotNull(splitsSyncHelper);
        mChangeNumber = since;
        mRbsChangeNumber = rbsSince;
        mEventsManager = checkNotNull(eventsManager);
        mChangeChecker = new SplitsChangeChecker();
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {

        if (mChangeNumber == null || mChangeNumber == 0) {
            mChangeNumber = mSplitsStorage.getTill();
        }

        if (mRbsChangeNumber == null || mRbsChangeNumber == 0) {
            mRbsChangeNumber = mRuleBasedSegmentStorage.getChangeNumber();
        }

        long storedChangeNumber = mSplitsStorage.getTill();
        long storedRbsChangeNumber = mRuleBasedSegmentStorage.getChangeNumber();
        if (mChangeNumber <= storedChangeNumber && mRbsChangeNumber <= storedRbsChangeNumber) {
            Logger.d("Received change numbers are previous than stored ones. " +
                    "Avoiding update.");
            return SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC);
        }

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(new SplitsSyncHelper.SinceChangeNumbers(mChangeNumber, mRbsChangeNumber), ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);
        if (result.getStatus() == SplitTaskExecutionStatus.SUCCESS) {
            // Fire *_UPDATED events BEFORE sync complete. This order is important:
            // if we fire TARGETING_RULES_SYNC_COMPLETE first, it may trigger SDK_READY,
            // and then the *_UPDATED events would immediately trigger SDK_UPDATE during initial sync.
            if (mSplitsSyncHelper.splitsHaveChanged()) {
                EventMetadata metadata = createUpdatedFlagsMetadata();
                mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED, metadata);
            }

            if (mSplitsSyncHelper.ruleBasedSegmentsHaveChanged()) {
                mEventsManager.notifyInternalEvent(SplitInternalEvent.RULE_BASED_SEGMENTS_UPDATED);
            }

            // Fire sync complete AFTER update events
            EventMetadata syncMetadata = EventMetadataHelpers.createFreshInstallMetadata();
            mEventsManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE, syncMetadata);
        }
        return result;
    }

    private EventMetadata createUpdatedFlagsMetadata() {
        List<String> updatedSplitNames = mSplitsSyncHelper.getLastUpdatedFlagNames();
        Long changeNumber = mSplitsSyncHelper.getLastChangeNumber();
        return EventMetadataHelpers.createFlagUpdateMetadata(updatedSplitNames, changeNumber);
    }

    @VisibleForTesting
    public void setChangeChecker(SplitsChangeChecker changeChecker) {
        mChangeChecker = changeChecker;
    }
}
