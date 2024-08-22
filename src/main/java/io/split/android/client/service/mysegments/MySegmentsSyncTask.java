package io.split.android.client.service.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.AllSegmentsChange;
import io.split.android.client.dtos.SegmentsChange;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.http.HttpStatus;
import io.split.android.client.service.synchronizer.MySegmentsChangeChecker;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.logger.Logger;

public class MySegmentsSyncTask implements SplitTask {

    private final HttpFetcher<AllSegmentsChange> mMySegmentsFetcher;
    private final MySegmentsStorage mMySegmentsStorage;
    private final MySegmentsStorage mMyLargeSegmentsStorage;
    private final boolean mAvoidCache;
    private final SplitEventsManager mEventsManager;
    private final MySegmentsChangeChecker mMySegmentsChangeChecker;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final SplitTaskType mTaskType;
    private final SplitInternalEvent mUpdateEvent;
    private final SplitInternalEvent mFetchedEvent;
    private final OperationType mTelemetryOperationType;

    public MySegmentsSyncTask(@NonNull HttpFetcher<AllSegmentsChange> mySegmentsFetcher,
                              @NonNull MySegmentsStorage mySegmentsStorage,
                              @NonNull MySegmentsStorage myLargeSegmentsStorage,
                              boolean avoidCache,
                              SplitEventsManager eventsManager,
                              @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                              @NonNull MySegmentsSyncTaskConfig config) {
        mMySegmentsFetcher = checkNotNull(mySegmentsFetcher);
        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
        mMyLargeSegmentsStorage = checkNotNull(myLargeSegmentsStorage);
        mAvoidCache = avoidCache;
        mEventsManager = eventsManager;
        mMySegmentsChangeChecker = new MySegmentsChangeChecker();
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mTaskType = config.getTaskType();
        mUpdateEvent = config.getUpdateEvent();
        mFetchedEvent = config.getFetchedEvent();
        mTelemetryOperationType = config.getTelemetryOperationType();
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        long startTime = System.currentTimeMillis();
        long latency = 0;
        try {
            AllSegmentsChange response = mMySegmentsFetcher.execute(new HashMap<>(), getHeaders());

            long now = System.currentTimeMillis();
            latency = now - startTime;

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
                oldSegments = new ArrayList<>(mMyLargeSegmentsStorage.getAll());
                mMyLargeSegmentsStorage.set(largeSegmentsChange);
            }

            mTelemetryRuntimeProducer.recordSuccessfulSync(mTelemetryOperationType, now);

            Logger.v("New segments fetched: " + String.join(", ", mySegments));
            Logger.v("New large segments fetched: " + String.join(", ", myLargeSegments));
            fireMySegmentsUpdatedIfNeeded(oldSegments, mySegments, oldLargeSegments, myLargeSegments);
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
        if (mMySegmentsChangeChecker.mySegmentsHaveChanged(oldSegments, newSegments)) {
            mEventsManager.notifyInternalEvent(mUpdateEvent);
        } else {
            boolean largeSegmentsHaveChanged = mMySegmentsChangeChecker.mySegmentsHaveChanged(oldLargeSegments, newLargeSegments);
            if (largeSegmentsHaveChanged) {
                mEventsManager.notifyInternalEvent(SplitInternalEvent.MY_LARGE_SEGMENTS_UPDATED);
            } else {
                mEventsManager.notifyInternalEvent(mFetchedEvent);
            }
        }
    }
}
