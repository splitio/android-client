package io.split.android.client.service.splits;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.split.android.client.api.EventMetadata;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.events.metadata.EventMetadataHelpers;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.synchronizer.SplitsChangeChecker;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorageProducer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class SplitsSyncTask implements SplitTask {

    private final String mSplitsFilterQueryStringFromConfig;

    private final SplitsStorage mSplitsStorage;
    private final RuleBasedSegmentStorageProducer mRuleBasedSegmentStorage;
    private final SplitsSyncHelper mSplitsSyncHelper;
    @Nullable
    private final ISplitEventsManager mEventsManager; // Should only be null on background sync
    private final SplitsChangeChecker mChangeChecker;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final int mOnDemandFetchBackoffMaxRetries;

    public static SplitsSyncTask build(@NonNull SplitsSyncHelper splitsSyncHelper,
                                       @NonNull SplitsStorage splitsStorage,
                                       @NonNull RuleBasedSegmentStorageProducer ruleBasedSegmentStorage,
                                       String splitsFilterQueryString,
                                       @NonNull ISplitEventsManager eventsManager,
                                       @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        return new SplitsSyncTask(splitsSyncHelper, splitsStorage, ruleBasedSegmentStorage, splitsFilterQueryString, telemetryRuntimeProducer, eventsManager, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);
    }

    public static SplitTask buildForBackground(@NonNull SplitsSyncHelper splitsSyncHelper,
                                               @NonNull SplitsStorage splitsStorage,
                                               @NonNull RuleBasedSegmentStorageProducer ruleBasedSegmentStorage,
                                               String splitsFilterQueryString,
                                               @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        return new SplitsSyncTask(splitsSyncHelper, splitsStorage, ruleBasedSegmentStorage, splitsFilterQueryString, telemetryRuntimeProducer, null, 1);
    }

    private SplitsSyncTask(@NonNull SplitsSyncHelper splitsSyncHelper,
                           @NonNull SplitsStorage splitsStorage,
                           @NonNull RuleBasedSegmentStorageProducer ruleBasedSegmentStorage,
                           String splitsFilterQueryString,
                           @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                           @Nullable ISplitEventsManager eventsManager,
                           int onDemandFetchBackoffMaxRetries) {

        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitsSyncHelper = checkNotNull(splitsSyncHelper);
        mRuleBasedSegmentStorage = checkNotNull(ruleBasedSegmentStorage);
        mSplitsFilterQueryStringFromConfig = splitsFilterQueryString;
        mEventsManager = eventsManager;
        mChangeChecker = new SplitsChangeChecker();
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mOnDemandFetchBackoffMaxRetries = onDemandFetchBackoffMaxRetries;
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        long storedChangeNumber = mSplitsStorage.getTill();
        long storedRbsChangeNumber = mRuleBasedSegmentStorage.getChangeNumber();

        boolean splitsFilterHasChanged = splitsFilterHasChanged(mSplitsStorage.getSplitsFilterQueryString());

        if (splitsFilterHasChanged) {
            mSplitsStorage.updateSplitsFilterQueryString(mSplitsFilterQueryStringFromConfig);
            storedChangeNumber = -1;
        }

        long startTime = System.currentTimeMillis();
        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(new SplitsSyncHelper.SinceChangeNumbers(storedChangeNumber, storedRbsChangeNumber),
                splitsFilterHasChanged,
                splitsFilterHasChanged, mOnDemandFetchBackoffMaxRetries);
        mTelemetryRuntimeProducer.recordSyncLatency(OperationType.SPLITS, System.currentTimeMillis() - startTime);

        if (result.getStatus() == SplitTaskExecutionStatus.SUCCESS) {
            mTelemetryRuntimeProducer.recordSuccessfulSync(OperationType.SPLITS, System.currentTimeMillis());
            notifyInternalEvent(storedChangeNumber);
        }

        return result;
    }

    private void notifyInternalEvent(long storedChangeNumber) {
        if (mEventsManager == null) {
            return;
        }

        // Before SDK_READY: fire SYNC_COMPLETE (initial load)
        // After SDK_READY: fire UPDATED if data changed (update)
        boolean sdkReady = mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY);

        if (!sdkReady) {
            EventMetadata syncMetadata = EventMetadataHelpers.createCacheReadyMetadata(null, true);
            mEventsManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE, syncMetadata);
            return;
        }

        // SDK is ready - check for actual updates
        if (mSplitsSyncHelper.splitsHaveChanged()) {
            EventMetadata metadata = createUpdatedFlagsMetadata();
            mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED, metadata);
        }

        if (mSplitsSyncHelper.ruleBasedSegmentsHaveChanged()) {
            mEventsManager.notifyInternalEvent(SplitInternalEvent.RULE_BASED_SEGMENTS_UPDATED);
        }
    }

    private EventMetadata createUpdatedFlagsMetadata() {
        List<String> updatedSplitNames = mSplitsSyncHelper.getLastUpdatedFlagNames();
        return EventMetadataHelpers.createUpdatedFlagsMetadata(updatedSplitNames);
    }

    private boolean splitsFilterHasChanged(String storedSplitsFilterQueryString) {
        return !sanitizeString(mSplitsFilterQueryStringFromConfig).equals(sanitizeString(storedSplitsFilterQueryString));
    }

    private String sanitizeString(String string) {
        return string != null ? string : "";
    }
}
