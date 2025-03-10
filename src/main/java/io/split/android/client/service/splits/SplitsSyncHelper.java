package io.split.android.client.service.splits;

import static io.split.android.client.service.ServiceConstants.FLAGS_SPEC_PARAM;
import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.split.android.client.dtos.RuleBasedSegmentChange;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.TargetingRulesChange;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.http.HttpStatus;
import io.split.android.client.service.rules.ProcessedRuleBasedSegmentChange;
import io.split.android.client.service.rules.RuleBasedSegmentChangeProcessor;
import io.split.android.client.service.sseclient.BackoffCounter;
import io.split.android.client.service.sseclient.ReconnectBackoffCounter;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorageProducer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.logger.Logger;

public class SplitsSyncHelper {

    private static final String SINCE_PARAM = "since";
    private static final String TILL_PARAM = "till";
    private static final String RBS_SINCE_PARAM = "rbSince";
    private static final int ON_DEMAND_FETCH_BACKOFF_MAX_WAIT = ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_WAIT;

    private final HttpFetcher<TargetingRulesChange> mSplitFetcher;
    private final SplitsStorage mSplitsStorage;
    private final SplitChangeProcessor mSplitChangeProcessor;
    private final RuleBasedSegmentChangeProcessor mRuleBasedSegmentChangeProcessor;
    private final RuleBasedSegmentStorageProducer mRuleBasedSegmentStorage;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final BackoffCounter mBackoffCounter;
    private final String mFlagsSpec;

    public SplitsSyncHelper(@NonNull HttpFetcher<TargetingRulesChange> splitFetcher,
                            @NonNull SplitsStorage splitsStorage,
                            @NonNull SplitChangeProcessor splitChangeProcessor,
                            @NonNull RuleBasedSegmentChangeProcessor ruleBasedSegmentChangeProcessor,
                            @NonNull RuleBasedSegmentStorageProducer ruleBasedSegmentStorage,
                            @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                            @Nullable String flagsSpec) {
        this(splitFetcher,
                splitsStorage,
                splitChangeProcessor,
                ruleBasedSegmentChangeProcessor,
                ruleBasedSegmentStorage,
                telemetryRuntimeProducer,
                new ReconnectBackoffCounter(1, ON_DEMAND_FETCH_BACKOFF_MAX_WAIT),
                flagsSpec);
    }

    @VisibleForTesting
    public SplitsSyncHelper(@NonNull HttpFetcher<TargetingRulesChange> splitFetcher,
                            @NonNull SplitsStorage splitsStorage,
                            @NonNull SplitChangeProcessor splitChangeProcessor,
                            @NonNull RuleBasedSegmentChangeProcessor ruleBasedSegmentChangeProcessor,
                            @NonNull RuleBasedSegmentStorageProducer ruleBasedSegmentStorage,
                            @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                            @NonNull BackoffCounter backoffCounter,
                            @Nullable String flagsSpec) {
        mSplitFetcher = checkNotNull(splitFetcher);
        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitChangeProcessor = checkNotNull(splitChangeProcessor);
        mRuleBasedSegmentChangeProcessor = checkNotNull(ruleBasedSegmentChangeProcessor);
        mRuleBasedSegmentStorage = checkNotNull(ruleBasedSegmentStorage);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mBackoffCounter = checkNotNull(backoffCounter);
        mFlagsSpec = flagsSpec;
    }

    public SplitTaskExecutionInfo sync(SinceChangeNumbers till, int onDemandFetchBackoffMaxRetries) {
        return sync(till, false, true, false, onDemandFetchBackoffMaxRetries);
    }

    public SplitTaskExecutionInfo sync(SinceChangeNumbers till, boolean clearBeforeUpdate, boolean resetChangeNumber, int onDemandFetchBackoffMaxRetries) {
        return sync(till, clearBeforeUpdate, false, resetChangeNumber, onDemandFetchBackoffMaxRetries);
    }

    private SplitTaskExecutionInfo sync(SinceChangeNumbers till, boolean clearBeforeUpdate, boolean avoidCache, boolean resetChangeNumber, int onDemandFetchBackoffMaxRetries) {
        try {
            boolean successfulSync = attemptSplitSync(till, clearBeforeUpdate, avoidCache, false, resetChangeNumber, onDemandFetchBackoffMaxRetries);

            if (!successfulSync) {
                attemptSplitSync(till, clearBeforeUpdate, avoidCache, true, resetChangeNumber, onDemandFetchBackoffMaxRetries);
            }
        } catch (HttpFetcherException e) {
            logError("Network error while fetching feature flags" + e.getLocalizedMessage());
            mTelemetryRuntimeProducer.recordSyncError(OperationType.SPLITS, e.getHttpStatus());

            HttpStatus httpStatus = HttpStatus.fromCode(e.getHttpStatus());
            if (httpStatus == HttpStatus.URI_TOO_LONG) {
                Logger.e("SDK initialization: the amount of flag sets provided is big, causing URI length error");
            }

            if (HttpStatus.isNotRetryable(httpStatus)) {
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
     * @param onDemandFetchBackoffMaxRetries max backoff retries for CDN bypass
     * @return whether sync finished successfully
     */
    private boolean attemptSplitSync(SinceChangeNumbers till, boolean clearBeforeUpdate, boolean avoidCache, boolean withCdnBypass, boolean resetChangeNumber, int onDemandFetchBackoffMaxRetries) throws Exception {
        int remainingAttempts = onDemandFetchBackoffMaxRetries;
        mBackoffCounter.resetCounter();
        while (true) {
            remainingAttempts--;

            SinceChangeNumbers changeNumber = fetchUntil(till, clearBeforeUpdate, avoidCache, withCdnBypass, resetChangeNumber);
            resetChangeNumber = false;

            if (till.getFlagsSince() <= changeNumber.getFlagsSince() && till.getRbsSince() <= changeNumber.getRbsSince()) {
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

    private SinceChangeNumbers fetchUntil(SinceChangeNumbers till, boolean clearBeforeUpdate, boolean avoidCache, boolean withCdnByPass, boolean resetChangeNumber) throws Exception {
        boolean shouldClearBeforeUpdate = clearBeforeUpdate;

        SinceChangeNumbers newTill = till;
        while (true) {
            long changeNumber = (resetChangeNumber) ? -1 : mSplitsStorage.getTill();
            long rbsChangeNumber = (resetChangeNumber) ? -1 : mRuleBasedSegmentStorage.getChangeNumber();
            resetChangeNumber = false;
            if (newTill.getFlagsSince() < changeNumber && newTill.getRbsSince() < rbsChangeNumber) {
                return new SinceChangeNumbers(changeNumber, rbsChangeNumber);
            }

            TargetingRulesChange targetingRulesChange = fetchSplits(new SinceChangeNumbers(changeNumber, rbsChangeNumber), avoidCache, withCdnByPass);
            SplitChange splitChange = targetingRulesChange.getFeatureFlagsChange();
            RuleBasedSegmentChange ruleBasedSegmentChange = targetingRulesChange.getRuleBasedSegmentsChange();
            updateStorage(shouldClearBeforeUpdate, splitChange, ruleBasedSegmentChange);
            shouldClearBeforeUpdate = false;

            newTill = new SinceChangeNumbers(splitChange.till, ruleBasedSegmentChange.getTill());
            if (splitChange.till == splitChange.since && ruleBasedSegmentChange.getTill() == ruleBasedSegmentChange.getSince()) {
                return new SinceChangeNumbers(splitChange.till, ruleBasedSegmentChange.getTill());
            }
        }
    }

    private TargetingRulesChange fetchSplits(SinceChangeNumbers till, boolean avoidCache, boolean withCdnByPass) throws HttpFetcherException {
        Map<String, Object> params = new LinkedHashMap<>();
        if (mFlagsSpec != null && !mFlagsSpec.trim().isEmpty()) {
            params.put(FLAGS_SPEC_PARAM, mFlagsSpec);
        }
        params.put(SINCE_PARAM, till.getFlagsSince());
        params.put(RBS_SINCE_PARAM, till.getRbsSince());

        if (withCdnByPass) {
            params.put(TILL_PARAM, till.getFlagsSince());
        }

        return mSplitFetcher.execute(params, getHeaders(avoidCache));
    }

    private void updateStorage(boolean clearBeforeUpdate, SplitChange splitChange, RuleBasedSegmentChange ruleBasedSegmentChange) {
        if (clearBeforeUpdate) {
            mSplitsStorage.clear();
            mRuleBasedSegmentStorage.clear();
        }
        mSplitsStorage.update(mSplitChangeProcessor.process(splitChange));
        updateRbsStorage(ruleBasedSegmentChange);
    }

    private void updateRbsStorage(RuleBasedSegmentChange ruleBasedSegmentChange) {
        ProcessedRuleBasedSegmentChange change = mRuleBasedSegmentChangeProcessor.process(ruleBasedSegmentChange.getSegments(), ruleBasedSegmentChange.getTill());
        mRuleBasedSegmentStorage.update(change.getActive(), change.getArchived(), change.getChangeNumber());
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

    public static class SinceChangeNumbers {
        private final long mFlagsSince;
        private final long mRbsSince;

        public SinceChangeNumbers(long flagsSince, long rbsSince) {
            mFlagsSince = flagsSince;
            mRbsSince = rbsSince;
        }

        public long getFlagsSince() {
            return mFlagsSince;
        }

        public long getRbsSince() {
            return mRbsSince;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof SinceChangeNumbers &&
                    mFlagsSince == ((SinceChangeNumbers) obj).mFlagsSince &&
                    mRbsSince == ((SinceChangeNumbers) obj).mRbsSince;
        }
    }
}
