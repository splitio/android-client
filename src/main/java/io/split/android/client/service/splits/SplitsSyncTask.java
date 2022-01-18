package io.split.android.client.service.splits;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.synchronizer.SplitsChangeChecker;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class SplitsSyncTask implements SplitTask {

    static final String SINCE_PARAM = "since";
    private final String mSplitsFilterQueryString;

    private final SplitsStorage mSplitsStorage;
    private final boolean mCheckCacheExpiration;
    private final long mCacheExpirationInSeconds;
    private final SplitsSyncHelper mSplitsSyncHelper;
    private final SplitEventsManager mEventsManager;
    private SplitsChangeChecker mChangeChecker;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    public SplitsSyncTask(@NonNull SplitsSyncHelper splitsSyncHelper,
                          @NonNull SplitsStorage splitsStorage,
                          boolean checkCacheExpiration,
                          long cacheExpirationInSeconds,
                          String splitsFilterQueryString,
                          @NonNull SplitEventsManager eventsManager,
                          @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {

        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitsSyncHelper = checkNotNull(splitsSyncHelper);
        mCacheExpirationInSeconds = cacheExpirationInSeconds;
        mCheckCacheExpiration = checkCacheExpiration;
        mSplitsFilterQueryString = splitsFilterQueryString;
        mEventsManager = checkNotNull(eventsManager);
        mChangeChecker = new SplitsChangeChecker();
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        long storedChangeNumber = mSplitsStorage.getTill();
        long updateTimestamp = mSplitsStorage.getUpdateTimestamp();
        String storedSplitsFilterQueryString = mSplitsStorage.getSplitsFilterQueryString();

        boolean shouldClearExpiredCache = mCheckCacheExpiration &&
                mSplitsSyncHelper.cacheHasExpired(storedChangeNumber, updateTimestamp, mCacheExpirationInSeconds);

        boolean splitsFilterHasChanged = splitsFilterHasChanged(storedSplitsFilterQueryString);

        if(splitsFilterHasChanged) {
            mSplitsStorage.updateSplitsFilterQueryString(mSplitsFilterQueryString);
            storedChangeNumber = -1;
        }
        Map<String, Object> params = new HashMap<>();
        params.put(SINCE_PARAM, storedChangeNumber);
        long startTime = System.currentTimeMillis();
        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(params, splitsFilterHasChanged || shouldClearExpiredCache, false);
        mTelemetryRuntimeProducer.recordSyncLatency(OperationType.SPLITS, System.currentTimeMillis() - startTime);
        if (result.getStatus() == SplitTaskExecutionStatus.SUCCESS) {
            SplitInternalEvent event = SplitInternalEvent.SPLITS_FETCHED;
            if (mChangeChecker.splitsHaveChanged(storedChangeNumber, mSplitsStorage.getTill())) {
                event = SplitInternalEvent.SPLITS_UPDATED;
            }
            mEventsManager.notifyInternalEvent(event);
        }

        return result;
    }

    private boolean splitsFilterHasChanged(String storedSplitsFilterQueryString) {
        return !sanitizeString(mSplitsFilterQueryString).equals(sanitizeString(storedSplitsFilterQueryString));
    }

    private String sanitizeString(String string) {
        return string != null ? string : "";
    }

    @VisibleForTesting
    public void setChangeChecker(SplitsChangeChecker changeChecker) {
        mChangeChecker = changeChecker;
    }
}
