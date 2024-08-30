package io.split.android.client.service.mysegments;

import static io.split.android.client.service.ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_WAIT;
import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.split.android.client.dtos.AllSegmentsChange;
import io.split.android.client.dtos.SegmentsChange;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.http.HttpStatus;
import io.split.android.client.service.sseclient.BackoffCounter;
import io.split.android.client.service.sseclient.ReconnectBackoffCounter;
import io.split.android.client.service.synchronizer.MySegmentsChangeChecker;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.Utils;
import io.split.android.client.utils.logger.Logger;

public class MySegmentsSyncTask implements SplitTask {

    private static final String TILL_PARAM = "till";

    private final HttpFetcher<AllSegmentsChange> mMySegmentsFetcher;

    private final MySegmentsStorage mMySegmentsStorage;
    private final MySegmentsStorage mMyLargeSegmentsStorage;
    private final SplitEventsManager mEventsManager;
    private final MySegmentsChangeChecker mMySegmentsChangeChecker;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final BackoffCounter mBackoffCounter;

    private final SplitTaskType mTaskType;
    private final SplitInternalEvent mUpdateEvent;
    private final SplitInternalEvent mFetchedEvent;
    private final OperationType mTelemetryOperationType;

    private final boolean mAvoidCache;
    @Nullable
    private final Long mTargetSegmentsChangeNumber;
    @Nullable
    private final Long mTargetLargeSegmentsChangeNumber;
    private final int mOnDemandFetchBackoffMaxRetries;

    public MySegmentsSyncTask(@NonNull HttpFetcher<AllSegmentsChange> mySegmentsFetcher,
                              @NonNull MySegmentsStorage mySegmentsStorage,
                              @NonNull MySegmentsStorage myLargeSegmentsStorage,
                              boolean avoidCache,
                              SplitEventsManager eventsManager,
                              @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                              @NonNull MySegmentsSyncTaskConfig config,
                              @Nullable Long targetSegmentsChangeNumber,
                              @Nullable Long targetLargeSegmentsChangeNumber) {
        this(mySegmentsFetcher,
                mySegmentsStorage,
                myLargeSegmentsStorage,
                avoidCache,
                eventsManager,
                new MySegmentsChangeChecker(),
                telemetryRuntimeProducer,
                config,
                targetSegmentsChangeNumber,
                targetLargeSegmentsChangeNumber,
                new ReconnectBackoffCounter(1, ON_DEMAND_FETCH_BACKOFF_MAX_WAIT),
                ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);
    }

    @VisibleForTesting
    public MySegmentsSyncTask(@NonNull HttpFetcher<AllSegmentsChange> mySegmentsFetcher,
                              @NonNull MySegmentsStorage mySegmentsStorage,
                              @NonNull MySegmentsStorage myLargeSegmentsStorage,
                              boolean avoidCache,
                              SplitEventsManager eventsManager,
                              MySegmentsChangeChecker mySegmentsChangeChecker,
                              @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                              @NonNull MySegmentsSyncTaskConfig config,
                              @Nullable Long targetSegmentsChangeNumber,
                              @Nullable Long targetLargeSegmentsChangeNumber,
                              BackoffCounter backoffCounter,
                              int onDemandFetchBackoffMaxRetries) {
        mMySegmentsFetcher = checkNotNull(mySegmentsFetcher);
        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
        mMyLargeSegmentsStorage = checkNotNull(myLargeSegmentsStorage);
        mAvoidCache = avoidCache;
        mEventsManager = eventsManager;
        mMySegmentsChangeChecker = mySegmentsChangeChecker;
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mTaskType = config.getTaskType();
        mUpdateEvent = config.getUpdateEvent();
        mFetchedEvent = config.getFetchedEvent();
        mTelemetryOperationType = config.getTelemetryOperationType();
        mTargetSegmentsChangeNumber = targetSegmentsChangeNumber;
        mTargetLargeSegmentsChangeNumber = targetLargeSegmentsChangeNumber;
        mBackoffCounter = backoffCounter;
        mOnDemandFetchBackoffMaxRetries = onDemandFetchBackoffMaxRetries;
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        long startTime = System.currentTimeMillis();
        long latency = 0;
        try {
            // if target change number is outdated, we don't need to fetch
            if (targetChangeNumberIsOutdated()) {
                Logger.v("Target CN is outdated. Skipping membership fetch");
                return SplitTaskExecutionInfo.success(mTaskType);
            }

            fetch(mOnDemandFetchBackoffMaxRetries);

            long now = System.currentTimeMillis();
            latency = now - startTime;

            mTelemetryRuntimeProducer.recordSuccessfulSync(mTelemetryOperationType, now);
        } catch (HttpFetcherException e) {
            logError("Network error while retrieving memberships: " + e.getLocalizedMessage());
            mTelemetryRuntimeProducer.recordSyncError(mTelemetryOperationType, e.getHttpStatus());

            if (HttpStatus.isNotRetryable(HttpStatus.fromCode(e.getHttpStatus()))) {
                return SplitTaskExecutionInfo.error(mTaskType,
                        Collections.singletonMap(SplitTaskExecutionInfo.DO_NOT_RETRY, true));
            }

            return SplitTaskExecutionInfo.error(mTaskType);
        } catch (Exception e) {
            logError("Unknown error while retrieving memberships: " + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(mTaskType);
        } finally {
            mTelemetryRuntimeProducer.recordSyncLatency(mTelemetryOperationType, latency);
        }
        Logger.d("My Segments have been updated");
        return SplitTaskExecutionInfo.success(mTaskType);
    }

    private boolean targetChangeNumberIsOutdated() {
        if (mTargetSegmentsChangeNumber == null && mTargetLargeSegmentsChangeNumber == null) {
            return false;
        }

        long segmentsTarget = Utils.getOrDefault(mTargetSegmentsChangeNumber, -1L);
        long largeSegmentsTarget = Utils.getOrDefault(mTargetLargeSegmentsChangeNumber, -1L);

        long msStorageChangeNumber = mMySegmentsStorage.getTill();
        long lsStorageChangeNumber = mMyLargeSegmentsStorage.getTill();

        return segmentsTarget <= msStorageChangeNumber && largeSegmentsTarget <= lsStorageChangeNumber;
    }

    private void fetch(int initialRetries) throws HttpFetcherException, InterruptedException {
        int remainingRetries = initialRetries;
        mBackoffCounter.resetCounter();
        while (remainingRetries > 0) {
            AllSegmentsChange response = mMySegmentsFetcher.execute(getParams(false), getHeaders());
            if (response == null) {
                throw new HttpFetcherException("", "Response is null");
            }

            if (isStaleResponse(response)) {
                Logger.d("Retrying memberships fetch due to change number mismatch");
                long waitMillis = TimeUnit.SECONDS.toMillis(mBackoffCounter.getNextRetryTime());
                Thread.sleep(waitMillis);
                remainingRetries--;
            } else {
                updateStorage(response);
                return;
            }
        }

        AllSegmentsChange response = mMySegmentsFetcher.execute(getParams(true), getHeaders());
        if (response == null) {
            throw new HttpFetcherException("", "Response is null");
        }

        updateStorage(response);
    }

    private Map<String, Object> getParams(boolean addTill) {
        Map<String, Object> params = new HashMap<>();
        if (addTill) {
            params.put(TILL_PARAM, Math.max(
                    Utils.getOrDefault(mTargetSegmentsChangeNumber, -1L),
                    Utils.getOrDefault(mTargetLargeSegmentsChangeNumber, -1L)));
        }

        return params;
    }

    private boolean isStaleResponse(@NonNull AllSegmentsChange response) {
        boolean segmentsTargetMatched = targetMatched(mTargetSegmentsChangeNumber, response.getSegmentsChange());
        boolean largeSegmentsTargetMatched = targetMatched(mTargetLargeSegmentsChangeNumber, response.getLargeSegmentsChange());

        return !segmentsTargetMatched || !largeSegmentsTargetMatched;
    }

    private boolean targetMatched(@Nullable Long targetChangeNumber, SegmentsChange change) {
        Long target = Utils.getOrDefault(targetChangeNumber, -1L);
        return target == -1 ||
                change == null ||
                change.getChangeNumber() == null ||
                change.getChangeNumber() != null && target <= change.getChangeNumber();
    }

    private void updateStorage(AllSegmentsChange response) {
        UpdateSegmentsResult segmentsResult = updateSegments(response.getSegmentsChange(), mMySegmentsStorage);
        UpdateSegmentsResult largeSegmentsResult = updateSegments(response.getLargeSegmentsChange(), mMyLargeSegmentsStorage);
        fireMySegmentsUpdatedIfNeeded(segmentsResult, largeSegmentsResult);
    }

    @NonNull
    private static UpdateSegmentsResult updateSegments(SegmentsChange segmentsChange, MySegmentsStorage storage) {
        List<String> oldSegments = new ArrayList<>();
        List<String> mySegments = new ArrayList<>();
        if (segmentsChange != null) {
            oldSegments = new ArrayList<>(storage.getAll());
            mySegments = segmentsChange.getNames();
            storage.set(segmentsChange);
        }
        return new UpdateSegmentsResult(oldSegments, mySegments);
    }

    private void logError(String message) {
        Logger.e("Error while executing memberships sync task: " + message);
    }

    private @Nullable Map<String, String> getHeaders() {
        if (mAvoidCache) {
            return SplitHttpHeadersBuilder.noCacheHeaders();
        }
        return null;
    }

    private void fireMySegmentsUpdatedIfNeeded(UpdateSegmentsResult segmentsResult, UpdateSegmentsResult largeSegmentsResult) {
        if (mEventsManager == null) {
            return;
        }

        // MY_SEGMENTS_UPDATED event when segments have changed
        boolean segmentsHaveChanged = mMySegmentsChangeChecker.mySegmentsHaveChanged(segmentsResult.oldSegments, segmentsResult.newSegments);
        boolean largeSegmentsHaveChanged = mMySegmentsChangeChecker.mySegmentsHaveChanged(largeSegmentsResult.oldSegments, largeSegmentsResult.newSegments);

        if (segmentsHaveChanged) {
            mEventsManager.notifyInternalEvent(mUpdateEvent);
        } else {
            // MY_LARGE_SEGMENTS_UPDATED event when large segments have changed
            if (largeSegmentsHaveChanged) {
                mEventsManager.notifyInternalEvent(SplitInternalEvent.MY_LARGE_SEGMENTS_UPDATED);
            } else {
                // otherwise, MY_SEGMENTS_FETCHED event
                mEventsManager.notifyInternalEvent(mFetchedEvent);
            }
        }
    }

    private static class UpdateSegmentsResult {
        public final List<String> oldSegments;
        public final List<String> newSegments;

        private UpdateSegmentsResult(List<String> oldSegments, List<String> newSegments) {
            this.oldSegments = oldSegments;
            this.newSegments = newSegments;
        }
    }
}
