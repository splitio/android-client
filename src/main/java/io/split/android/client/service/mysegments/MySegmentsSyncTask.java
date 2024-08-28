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
            fetch(mOnDemandFetchBackoffMaxRetries);

            long now = System.currentTimeMillis();
            latency = now - startTime;

            mTelemetryRuntimeProducer.recordSuccessfulSync(mTelemetryOperationType, now);
        } catch (HttpFetcherException e) {
            logError("Network error while retrieving my segments: " + e.getLocalizedMessage());
            mTelemetryRuntimeProducer.recordSyncError(mTelemetryOperationType, e.getHttpStatus());

            if (HttpStatus.isNotRetryable(HttpStatus.fromCode(e.getHttpStatus()))) {
                return SplitTaskExecutionInfo.error(mTaskType,
                        Collections.singletonMap(SplitTaskExecutionInfo.DO_NOT_RETRY, true));
            }

            return SplitTaskExecutionInfo.error(mTaskType);
        } catch (Exception e) {
            logError("Unknown error while retrieving my segments: " + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(mTaskType);
        } finally {
            mTelemetryRuntimeProducer.recordSyncLatency(mTelemetryOperationType, latency);
        }
        Logger.d("My Segments have been updated");
        return SplitTaskExecutionInfo.success(mTaskType);
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
            long segmentsTarget = Utils.getOrDefault(mTargetSegmentsChangeNumber, -1L);
            long largeSegmentsTarget = Utils.getOrDefault(mTargetLargeSegmentsChangeNumber, -1L);
            params.put(TILL_PARAM, Math.max(segmentsTarget, largeSegmentsTarget));
        }

        return params;
    }

    private boolean isStaleResponse(AllSegmentsChange response) {
        boolean checkSegments = Utils.getOrDefault(mTargetSegmentsChangeNumber, -1L) != -1;
        boolean checkLargeSegments = Utils.getOrDefault(mTargetLargeSegmentsChangeNumber, -1L) != -1;

        boolean segmentsTargetMatched = !checkSegments ||
                response.getSegmentsChange() != null && mTargetSegmentsChangeNumber.equals(response.getSegmentsChange().getChangeNumber());
        boolean largeSegmentsTargetMatched = !checkLargeSegments ||
                response.getLargeSegmentsChange() != null && mTargetLargeSegmentsChangeNumber.equals(response.getLargeSegmentsChange().getChangeNumber());

        if (!segmentsTargetMatched) {
            Logger.v("Segments target change number not matched. Expected: " + mTargetSegmentsChangeNumber + " - Actual: " + response.getSegmentsChange().getChangeNumber());
        }

        if (!largeSegmentsTargetMatched) {
            Logger.v("Large segments target change number not matched. Expected: " + mTargetLargeSegmentsChangeNumber + " - Actual: " + response.getLargeSegmentsChange().getChangeNumber());
        }

        return !segmentsTargetMatched || !largeSegmentsTargetMatched;
    }

    private void updateStorage(AllSegmentsChange response) {
        List<String> oldSegments = new ArrayList<>();
        List<String> mySegments = new ArrayList<>();
        SegmentsChange segmentsChange = response.getSegmentsChange();
        if (segmentsChange != null) {
            oldSegments = new ArrayList<>(mMySegmentsStorage.getAll());
            mySegments = segmentsChange.getNames();
            mMySegmentsStorage.set(segmentsChange);
        }

        List<String> oldLargeSegments = new ArrayList<>();
        List<String> myLargeSegments = new ArrayList<>();
        SegmentsChange largeSegmentsChange = response.getLargeSegmentsChange();
        if (largeSegmentsChange != null) {
            myLargeSegments = largeSegmentsChange.getNames();
            oldLargeSegments = new ArrayList<>(mMyLargeSegmentsStorage.getAll());
            mMyLargeSegmentsStorage.set(largeSegmentsChange);
        }
        fireMySegmentsUpdatedIfNeeded(oldSegments, mySegments, oldLargeSegments, myLargeSegments);
    }

    private void logError(String message) {
        Logger.e("Error while executing my segments sync task: " + message);
    }

    private @Nullable Map<String, String> getHeaders() {
        if (mAvoidCache) {
            return SplitHttpHeadersBuilder.noCacheHeaders();
        }
        return null;
    }

    private void fireMySegmentsUpdatedIfNeeded(List<String> oldSegments, List<String> newSegments, List<String> oldLargeSegments, List<String> newLargeSegments) {
        if (mEventsManager == null) {
            return;
        }

        // MY_SEGMENTS_UPDATED event when segments have changed
        boolean segmentsHaveChanged = mMySegmentsChangeChecker.mySegmentsHaveChanged(oldSegments, newSegments);
        boolean largeSegmentsHaveChanged = mMySegmentsChangeChecker.mySegmentsHaveChanged(oldLargeSegments, newLargeSegments);

        if (segmentsHaveChanged) {
            Logger.v("New segments fetched: " + String.join(", ", newSegments));
        }
        if (largeSegmentsHaveChanged) {
            Logger.v("New large segments fetched: " + String.join(", ", newLargeSegments));
        }

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
}
