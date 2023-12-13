package io.split.android.client.service.splits;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.http.HttpStatus;
import io.split.android.client.service.sseclient.BackoffCounter;
import io.split.android.client.service.sseclient.ReconnectBackoffCounter;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.logger.Logger;

public class SplitsSyncHelper {

    private static final String SINCE_PARAM = "since";
    private static final String TILL_PARAM = "till";
    private static final int ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES = 10;
    private static final int ON_DEMAND_FETCH_BACKOFF_MAX_WAIT = ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_WAIT;

    private final HttpFetcher<SplitChange> mSplitFetcher;
    private final SplitsStorage mSplitsStorage;
    private final SplitChangeProcessor mSplitChangeProcessor;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final BackoffCounter mBackoffCounter;

    public SplitsSyncHelper(@NonNull HttpFetcher<SplitChange> splitFetcher,
                            @NonNull SplitsStorage splitsStorage,
                            @NonNull SplitChangeProcessor splitChangeProcessor,
                            @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        this(splitFetcher,
                splitsStorage,
                splitChangeProcessor,
                telemetryRuntimeProducer,
                new ReconnectBackoffCounter(1, ON_DEMAND_FETCH_BACKOFF_MAX_WAIT));
    }

    @VisibleForTesting
    public SplitsSyncHelper(@NonNull HttpFetcher<SplitChange> splitFetcher,
                            @NonNull SplitsStorage splitsStorage,
                            @NonNull SplitChangeProcessor splitChangeProcessor,
                            @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                            @NonNull BackoffCounter backoffCounter) {
        mSplitFetcher = checkNotNull(splitFetcher);
        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitChangeProcessor = checkNotNull(splitChangeProcessor);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mBackoffCounter = checkNotNull(backoffCounter);
    }

    public SplitTaskExecutionInfo sync(long till) {
        return sync(till, false, true, false);
    }

    public SplitTaskExecutionInfo sync(long till, boolean clearBeforeUpdate, boolean resetChangeNumber) {
        return sync(till, clearBeforeUpdate, false, resetChangeNumber);
    }

    private SplitTaskExecutionInfo sync(long till, boolean clearBeforeUpdate, boolean avoidCache, boolean resetChangeNumber) {
        try {
            boolean successfulSync = attemptSplitSync(till, clearBeforeUpdate, avoidCache, false, resetChangeNumber);

            if (!successfulSync) {
                attemptSplitSync(till, clearBeforeUpdate, avoidCache, true, resetChangeNumber);
            }
        } catch (HttpFetcherException e) {
            logError("Network error while fetching feature flags" + e.getLocalizedMessage());
            mTelemetryRuntimeProducer.recordSyncError(OperationType.SPLITS, e.getHttpStatus());

            if (HttpStatus.fromCode(e.getHttpStatus()) == HttpStatus.URI_TOO_LONG) {
                Logger.e("SDK initialization: the amount of flag sets provided is big, causing URI length error");
                return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC,
                        Collections.singletonMap(SplitTaskExecutionInfo.DO_NOT_RETRY, true));
            }

            return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC);
        } catch (Exception e) {
            logError("Unexpected while fetching feature flags" + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC);
        }

        Logger.d("Feature flags have been updated");
        return SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC);
    }

    /**
     * @param till              target changeNumber
     * @param clearBeforeUpdate whether to clear splits storage before updating it
     * @param avoidCache        whether to send no-cache header to api
     * @param withCdnBypass     whether to add additional query param to bypass CDN
     * @return whether sync finished successfully
     */
    private boolean attemptSplitSync(long till, boolean clearBeforeUpdate, boolean avoidCache, boolean withCdnBypass, boolean resetChangeNumber) throws Exception {
        int remainingAttempts = ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES;
        mBackoffCounter.resetCounter();
        while (true) {
            remainingAttempts--;

            long changeNumber = fetchUntil(till, clearBeforeUpdate, avoidCache, withCdnBypass, resetChangeNumber);
            resetChangeNumber = false;

            if (till <= changeNumber) {
                return true;
            }

            if (remainingAttempts <= 0) {
                return false;
            }

            try {
                long backoffPeriod = TimeUnit.SECONDS.toMillis(mBackoffCounter.getNextRetryTime());
                Thread.sleep(backoffPeriod);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                Logger.e("Interrupted while waiting for next retry");
            }
        }
    }

    private long fetchUntil(long till, boolean clearBeforeUpdate, boolean avoidCache, boolean withCdnByPass, boolean resetChangeNumber) throws Exception {
        boolean shouldClearBeforeUpdate = clearBeforeUpdate;

        while (true) {
            long changeNumber = (resetChangeNumber) ? -1 : mSplitsStorage.getTill();
            resetChangeNumber = false;
            if (till < changeNumber) {
                return changeNumber;
            }

            SplitChange splitChange = fetchSplits(changeNumber, avoidCache, withCdnByPass);
            updateStorage(shouldClearBeforeUpdate, splitChange);
            shouldClearBeforeUpdate = false;

            if (splitChange.till == splitChange.since) {
                return splitChange.till;
            }
        }
    }

    private SplitChange fetchSplits(long till, boolean avoidCache, boolean withCdnByPass) throws HttpFetcherException {
        Map<String, Object> params = new HashMap<>();
        params.put(SINCE_PARAM, till);

        if (withCdnByPass) {
            params.put(TILL_PARAM, till);
        }

        return mSplitFetcher.execute(params, getHeaders(avoidCache));
    }

    private void updateStorage(boolean clearBeforeUpdate, SplitChange splitChange) {
        if (clearBeforeUpdate) {
            mSplitsStorage.clear();
        }
        mSplitsStorage.update(mSplitChangeProcessor.process(splitChange));
    }

    public boolean cacheHasExpired(long storedChangeNumber, long updateTimestamp, long cacheExpirationInSeconds) {
        long elapsed = now() - updateTimestamp;
        return storedChangeNumber > -1
                && updateTimestamp > 0
                && (elapsed > cacheExpirationInSeconds);
    }

    private long now() {
        return System.currentTimeMillis() / 1000;
    }

    private void logError(String message) {
        Logger.e("Error while executing splits sync/update task: " + message);
    }

    private @Nullable Map<String, String> getHeaders(boolean avoidCache) {
        if (avoidCache) {
            return SplitHttpHeadersBuilder.noCacheHeaders();
        }
        return null;
    }
}
