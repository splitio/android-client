package io.split.android.client.service.splits;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.synchronizer.SplitsChangeChecker;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class SplitsSyncTask implements SplitTask {

    private final String mSplitsFilterQueryStringFromConfig;

    private final SplitsStorage mSplitsStorage;
    private final SplitsSyncHelper mSplitsSyncHelper;
    @Nullable
    private final ISplitEventsManager mEventsManager; // Should only be null on background sync
    private final SplitsChangeChecker mChangeChecker;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final int mOnDemandFetchBackoffMaxRetries;

    public static SplitsSyncTask build(@NonNull SplitsSyncHelper splitsSyncHelper,
                                       @NonNull SplitsStorage splitsStorage,
                                       String splitsFilterQueryString,
                                       @NonNull ISplitEventsManager eventsManager,
                                       @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        return new SplitsSyncTask(splitsSyncHelper, splitsStorage, splitsFilterQueryString, telemetryRuntimeProducer, eventsManager, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);
    }

    public static SplitTask buildForBackground(@NonNull SplitsSyncHelper splitsSyncHelper,
                                                    @NonNull SplitsStorage splitsStorage,
                                               String splitsFilterQueryString,
                                                    @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        return new SplitsSyncTask(splitsSyncHelper, splitsStorage, splitsFilterQueryString, telemetryRuntimeProducer, null, 1);
    }

    private SplitsSyncTask(@NonNull SplitsSyncHelper splitsSyncHelper,
                           @NonNull SplitsStorage splitsStorage,
                           String splitsFilterQueryString,
                           @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                           @Nullable ISplitEventsManager eventsManager,
                           int onDemandFetchBackoffMaxRetries) {

        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitsSyncHelper = checkNotNull(splitsSyncHelper);
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

        boolean splitsFilterHasChanged = splitsFilterHasChanged(mSplitsStorage.getSplitsFilterQueryString());

        if (splitsFilterHasChanged) {
            mSplitsStorage.updateSplitsFilterQueryString(mSplitsFilterQueryStringFromConfig);
            storedChangeNumber = -1;
        }

        long startTime = System.currentTimeMillis();
        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(storedChangeNumber,
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
        if (mEventsManager != null) {
            SplitInternalEvent event = SplitInternalEvent.SPLITS_FETCHED;
            if (mChangeChecker.splitsHaveChanged(storedChangeNumber, mSplitsStorage.getTill())) {
                event = SplitInternalEvent.SPLITS_UPDATED;
            }

            mEventsManager.notifyInternalEvent(event);
        }
    }

    private boolean splitsFilterHasChanged(String storedSplitsFilterQueryString) {
        return !sanitizeString(mSplitsFilterQueryStringFromConfig).equals(sanitizeString(storedSplitsFilterQueryString));
    }

    private String sanitizeString(String string) {
        return string != null ? string : "";
    }
}
