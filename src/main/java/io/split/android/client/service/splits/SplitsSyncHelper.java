package io.split.android.client.service.splits;

import static io.split.android.client.service.ServiceConstants.FLAGS_SPEC_PARAM;
import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import io.split.android.client.storage.general.GeneralInfoStorage;
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
    private static final long DEFAULT_PROXY_CHECK_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(1);

    private final HttpFetcher<TargetingRulesChange> mSplitFetcher;
    private final SplitsStorage mSplitsStorage;
    private final SplitChangeProcessor mSplitChangeProcessor;
    private final RuleBasedSegmentChangeProcessor mRuleBasedSegmentChangeProcessor;
    private final RuleBasedSegmentStorageProducer mRuleBasedSegmentStorage;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final BackoffCounter mBackoffCounter;
    private final OutdatedSplitProxyHandler mOutdatedSplitProxyHandler;
    private final ExecutorService mExecutor;
    private final TargetingRulesCache mTargetingRulesCache;

    public SplitsSyncHelper(@NonNull HttpFetcher<TargetingRulesChange> splitFetcher,
                            @NonNull SplitsStorage splitsStorage,
                            @NonNull SplitChangeProcessor splitChangeProcessor,
                            @NonNull RuleBasedSegmentChangeProcessor ruleBasedSegmentChangeProcessor,
                            @NonNull RuleBasedSegmentStorageProducer ruleBasedSegmentStorage,
                            @NonNull GeneralInfoStorage generalInfoStorage,
                            @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                            @Nullable String flagsSpec,
                            boolean forBackgroundSync,
                            @Nullable TargetingRulesCache targetingRulesCache) {
        this(splitFetcher,
                splitsStorage,
                splitChangeProcessor,
                ruleBasedSegmentChangeProcessor,
                ruleBasedSegmentStorage,
                generalInfoStorage,
                telemetryRuntimeProducer,
                new ReconnectBackoffCounter(1, ON_DEMAND_FETCH_BACKOFF_MAX_WAIT),
                flagsSpec,
                forBackgroundSync,
                DEFAULT_PROXY_CHECK_INTERVAL_MILLIS,
                targetingRulesCache);
    }

    public SplitsSyncHelper(@NonNull HttpFetcher<TargetingRulesChange> splitFetcher,
                            @NonNull SplitsStorage splitsStorage,
                            @NonNull SplitChangeProcessor splitChangeProcessor,
                            @NonNull RuleBasedSegmentChangeProcessor ruleBasedSegmentChangeProcessor,
                            @NonNull RuleBasedSegmentStorageProducer ruleBasedSegmentStorage,
                            @NonNull GeneralInfoStorage generalInfoStorage,
                            @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                            @NonNull BackoffCounter backoffCounter,
                            @Nullable String flagsSpec,
                            @Nullable TargetingRulesCache targetingRulesCache) {
        this(splitFetcher,
                splitsStorage,
                splitChangeProcessor,
                ruleBasedSegmentChangeProcessor,
                ruleBasedSegmentStorage,
                generalInfoStorage,
                telemetryRuntimeProducer,
                backoffCounter,
                flagsSpec,
                false,
                DEFAULT_PROXY_CHECK_INTERVAL_MILLIS,
                targetingRulesCache);
    }

    @VisibleForTesting
    public SplitsSyncHelper(@NonNull HttpFetcher<TargetingRulesChange> splitFetcher,
                            @NonNull SplitsStorage splitsStorage,
                            @NonNull SplitChangeProcessor splitChangeProcessor,
                            @NonNull RuleBasedSegmentChangeProcessor ruleBasedSegmentChangeProcessor,
                            @NonNull RuleBasedSegmentStorageProducer ruleBasedSegmentStorage,
                            @NonNull GeneralInfoStorage generalInfoStorage,
                            @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                            @NonNull BackoffCounter backoffCounter,
                            @Nullable String flagsSpec,
                            boolean forBackgroundSync,
                            long proxyCheckIntervalMillis,
                            @Nullable TargetingRulesCache targetingRulesCache) {
        mSplitFetcher = checkNotNull(splitFetcher);
        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitChangeProcessor = checkNotNull(splitChangeProcessor);
        mRuleBasedSegmentChangeProcessor = checkNotNull(ruleBasedSegmentChangeProcessor);
        mRuleBasedSegmentStorage = checkNotNull(ruleBasedSegmentStorage);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mBackoffCounter = checkNotNull(backoffCounter);
        mOutdatedSplitProxyHandler = new OutdatedSplitProxyHandler(flagsSpec, forBackgroundSync, generalInfoStorage, proxyCheckIntervalMillis);
        mExecutor = Executors.newSingleThreadExecutor();
        mTargetingRulesCache = targetingRulesCache;
    }

    public SplitTaskExecutionInfo sync(SinceChangeNumbers till, int onDemandFetchBackoffMaxRetries) {
        return sync(till, false, true, false, onDemandFetchBackoffMaxRetries);
    }

    public SplitTaskExecutionInfo sync(SinceChangeNumbers till, boolean clearBeforeUpdate, boolean resetChangeNumber, int onDemandFetchBackoffMaxRetries) {
        return sync(till, clearBeforeUpdate, false, resetChangeNumber, onDemandFetchBackoffMaxRetries);
    }

    private SplitTaskExecutionInfo sync(SinceChangeNumbers till, boolean clearBeforeUpdate, boolean avoidCache, boolean resetChangeNumber, int onDemandFetchBackoffMaxRetries) {
        try {
            mOutdatedSplitProxyHandler.performProxyCheck();
            if (mOutdatedSplitProxyHandler.isRecoveryMode()) {
                clearBeforeUpdate = true;
                resetChangeNumber = true;
            }

            CdnByPassType cdnByPassType = attemptSplitSync(till, clearBeforeUpdate, avoidCache, CdnByPassType.NONE, resetChangeNumber, onDemandFetchBackoffMaxRetries);

            if (cdnByPassType != CdnByPassType.NONE) {
                attemptSplitSync(till, clearBeforeUpdate, avoidCache, cdnByPassType, resetChangeNumber, onDemandFetchBackoffMaxRetries);
            }
        } catch (HttpFetcherException e) {
            logError("Network error while fetching feature flags - " + e.getLocalizedMessage());
            mTelemetryRuntimeProducer.recordSyncError(OperationType.SPLITS, e.getHttpStatus());

            HttpStatus httpStatus = HttpStatus.fromCode(e.getHttpStatus());
            if (httpStatus == HttpStatus.URI_TOO_LONG) {
                Logger.e("SDK initialization: the amount of flag sets provided is big, causing URI length error");
            }

            if (HttpStatus.isNotRetryable(httpStatus)) {
                return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC,
                        Collections.singletonMap(SplitTaskExecutionInfo.DO_NOT_RETRY, true));
            }

            if (HttpStatus.isProxyOutdated(httpStatus)) {
                try {
                    mOutdatedSplitProxyHandler.trackProxyError();
                } catch (Exception e1) {
                    logError("Unexpected while handling outdated proxy " + e1.getLocalizedMessage());
                }
            }

            return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC);
        } catch (Exception e) {
            logError("Unexpected while fetching feature flags" + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC);
        }

        Logger.d("Feature flags have been updated");

        if (mOutdatedSplitProxyHandler.isRecoveryMode()) {
            Logger.i("Resetting proxy check timestamp due to successful recovery");
            mOutdatedSplitProxyHandler.resetProxyCheckTimestamp();
        }
        return SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC);
    }

    /**
     * @param targetChangeNumber             target changeNumber
     * @param clearBeforeUpdate              whether to clear splits storage before updating it
     * @param avoidCache                     whether to send no-cache header to api
     * @param withCdnBypass                  whether to add additional query param to bypass CDN
     * @param onDemandFetchBackoffMaxRetries max backoff retries for CDN bypass
     * @return whether sync finished successfully
     */
    private CdnByPassType attemptSplitSync(SinceChangeNumbers targetChangeNumber, boolean clearBeforeUpdate, boolean avoidCache, CdnByPassType withCdnBypass, boolean resetChangeNumber, int onDemandFetchBackoffMaxRetries) throws Exception {
        int remainingAttempts = onDemandFetchBackoffMaxRetries;
        mBackoffCounter.resetCounter();
        while (true) {
            remainingAttempts--;

            SinceChangeNumbers retrievedChangeNumber = fetchUntil(targetChangeNumber, clearBeforeUpdate, avoidCache, withCdnBypass, resetChangeNumber);
            resetChangeNumber = false;

            if (targetChangeNumber.getFlagsSince() <= retrievedChangeNumber.getFlagsSince() &&
                    targetChangeNumber.getRbsSince() != null && retrievedChangeNumber.getRbsSince() != null && targetChangeNumber.getRbsSince() <= retrievedChangeNumber.getRbsSince()) {
                return CdnByPassType.NONE;
            }

            if (remainingAttempts <= 0) {
                if (targetChangeNumber.getFlagsSince() <= retrievedChangeNumber.getFlagsSince()) {
                    return CdnByPassType.RBS;
                } else {
                    return CdnByPassType.FLAGS;
                }
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

    private SinceChangeNumbers fetchUntil(SinceChangeNumbers till, boolean clearBeforeUpdate, boolean avoidCache, CdnByPassType withCdnByPass, boolean resetChangeNumber) throws Exception {
        boolean shouldClearBeforeUpdate = clearBeforeUpdate;

        SinceChangeNumbers newTill = till;
        while (true) {
            long changeNumber = (resetChangeNumber) ? -1 : mSplitsStorage.getTill();
            long rbsChangeNumber = (resetChangeNumber) ? -1 : mRuleBasedSegmentStorage.getChangeNumber();
            resetChangeNumber = false;
            if ((newTill.getFlagsSince() < changeNumber) && ((newTill.getRbsSince() == null) || (newTill.getRbsSince() < rbsChangeNumber))) {
                return new SinceChangeNumbers(changeNumber, rbsChangeNumber);
            }

            TargetingRulesChange targetingRulesChange;
            // Try to use cached value for fresh installs
            if ((rbsChangeNumber == -1L) && changeNumber == -1 && mTargetingRulesCache != null) {
                targetingRulesChange = mTargetingRulesCache.getAndConsume();
                if (targetingRulesChange != null) {
                    Logger.d("Fresh install: Using prefetched targeting rules from cache");
                } else {
                    Logger.d("Fetching targeting rules - changeNumber: " + changeNumber + ", rbsChangeNumber: " + rbsChangeNumber);
                    targetingRulesChange = fetchSplits(new SinceChangeNumbers(changeNumber, rbsChangeNumber), avoidCache, withCdnByPass);
                }
            } else {
                Logger.d("Fetching targeting rules - changeNumber: " + changeNumber + ", rbsChangeNumber: " + rbsChangeNumber);
                targetingRulesChange = fetchSplits(new SinceChangeNumbers(changeNumber, rbsChangeNumber), avoidCache, withCdnByPass);
            }

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

    private TargetingRulesChange fetchSplits(SinceChangeNumbers till, boolean avoidCache, CdnByPassType cdnByPassType) throws HttpFetcherException {
        Map<String, Object> params = new LinkedHashMap<>();
        String flagsSpec = mOutdatedSplitProxyHandler.getCurrentSpec();
        if (flagsSpec != null && !flagsSpec.trim().isEmpty()) {
            params.put(FLAGS_SPEC_PARAM, flagsSpec);
        }
        params.put(SINCE_PARAM, till.getFlagsSince());
        if (!mOutdatedSplitProxyHandler.isFallbackMode() && till.getRbsSince() != null) {
            params.put(RBS_SINCE_PARAM, till.getRbsSince());
        }

        if (cdnByPassType == CdnByPassType.RBS) {
            params.put(TILL_PARAM, till.getRbsSince());
        } else if (cdnByPassType == CdnByPassType.FLAGS) {
            params.put(TILL_PARAM, till.getFlagsSince());
        }

        return mSplitFetcher.execute(params, getHeaders(avoidCache));
    }

    public static void fetchSplits(SinceChangeNumbers till,
                                   boolean avoidCache,
                                   String currentSpec,
                                   HttpFetcher<TargetingRulesChange> fetcher,
                                   @NonNull TargetingRulesCache cache) throws HttpFetcherException {
        try {
            cache.setWithLock(() -> {
                Map<String, Object> params = new LinkedHashMap<>();
                if (currentSpec != null && !currentSpec.trim().isEmpty()) {
                    params.put(FLAGS_SPEC_PARAM, currentSpec);
                }
                params.put(SINCE_PARAM, till.getFlagsSince());
                params.put(RBS_SINCE_PARAM, till.getRbsSince());

                return fetcher.execute(params, getHeaders(avoidCache));
            });
        } catch (HttpFetcherException e) {
            throw e;
        } catch (Exception e) {
            Logger.e("Unexpected error fetching splits: " + e.getMessage());
            throw new HttpFetcherException("splits", "Unexpected error: " + e.getMessage());
        }
    }

    private void updateStorage(boolean clearBeforeUpdate, SplitChange splitChange, RuleBasedSegmentChange ruleBasedSegmentChange) {
        if (clearBeforeUpdate) {
            mSplitsStorage.clear();
            mRuleBasedSegmentStorage.clear();
        }
        mSplitsStorage.update(mSplitChangeProcessor.process(splitChange), mExecutor);
        updateRbsStorage(ruleBasedSegmentChange);
    }

    private void updateRbsStorage(RuleBasedSegmentChange ruleBasedSegmentChange) {
        ProcessedRuleBasedSegmentChange change = mRuleBasedSegmentChangeProcessor.process(ruleBasedSegmentChange.getSegments(), ruleBasedSegmentChange.getTill());
        mRuleBasedSegmentStorage.update(change.getActive(), change.getArchived(), change.getChangeNumber(), mExecutor);
    }

    private void logError(String message) {
        Logger.e("Error while executing splits sync/update task: " + message);
    }

    @Nullable
    private static Map<String, String> getHeaders(boolean avoidCache) {
        if (avoidCache) {
            return SplitHttpHeadersBuilder.noCacheHeaders();
        }
        return null;
    }

    public static class SinceChangeNumbers {
        private final long mFlagsSince;
        @Nullable
        private final Long mRbsSince;

        public SinceChangeNumbers(long flagsSince, @Nullable Long rbsSince) {
            mFlagsSince = flagsSince;
            mRbsSince = rbsSince;
        }

        public long getFlagsSince() {
            return mFlagsSince;
        }

        @Nullable
        public Long getRbsSince() {
            return mRbsSince;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof SinceChangeNumbers &&
                    mFlagsSince == ((SinceChangeNumbers) obj).mFlagsSince &&
                    (mRbsSince == null && ((SinceChangeNumbers) obj).mRbsSince == null);
        }

        @NonNull
        @Override
        public String toString() {
            return "{" +
                    "ff=" + mFlagsSince +
                    ", rbs=" + mRbsSince +
                    '}';
        }
    }

    private enum CdnByPassType {
        NONE,
        FLAGS,
        RBS,
    }
}
