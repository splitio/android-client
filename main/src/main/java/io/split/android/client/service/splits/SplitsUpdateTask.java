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
            // Always fire TARGETING_RULES_SYNC_COMPLETE when sync succeeds
            // Sync path metadata: freshInstall=true (synced from network), timestamp=null
            EventMetadata syncMetadata = EventMetadataHelpers.createCacheReadyMetadata(null, true);
            mEventsManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE, syncMetadata);

            // Fire SPLITS_UPDATED only if data actually changed
            if (mSplitsSyncHelper.splitsHaveChanged()) {
                EventMetadata metadata = createUpdatedFlagsMetadata();
                mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED, metadata);
            }

            if (mSplitsSyncHelper.ruleBasedSegmentsHaveChanged()) {
                mEventsManager.notifyInternalEvent(SplitInternalEvent.RULE_BASED_SEGMENTS_UPDATED);
            }
        }
        return result;
    }

    private EventMetadata createUpdatedFlagsMetadata() {
        List<String> updatedSplitNames = mSplitsSyncHelper.getLastUpdatedFlagNames();
        return EventMetadataHelpers.createUpdatedFlagsMetadata(updatedSplitNames);
    }

    @VisibleForTesting
    public void setChangeChecker(SplitsChangeChecker changeChecker) {
        mChangeChecker = changeChecker;
    }
}
