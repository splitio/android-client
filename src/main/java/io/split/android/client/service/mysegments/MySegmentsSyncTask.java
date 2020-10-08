package io.split.android.client.service.mysegments;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.sseclient.ReconnectBackoffCounter;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class MySegmentsSyncTask implements SplitTask {

    private final HttpFetcher<List<MySegment>> mMySegmentsFetcher;
    private final MySegmentsStorage mMySegmentsStorage;
    private boolean mRetryOnFail;

    private static final int RETRY_BASE = 1;

    public MySegmentsSyncTask(@NonNull HttpFetcher<List<MySegment>> mySegmentsFetcher,
                              @NonNull MySegmentsStorage mySegmentsStorage,
                              boolean retryOnFail) {
        mMySegmentsFetcher = checkNotNull(mySegmentsFetcher);
        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
        mRetryOnFail = retryOnFail;
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        ReconnectBackoffCounter backoffCounter = new ReconnectBackoffCounter(RETRY_BASE);
        boolean success = false;

        while(!success) {
            try {
                List<MySegment> mySegments = mMySegmentsFetcher.execute(new HashMap<>());
                mMySegmentsStorage.set(getNameList(mySegments));
                success = true;
            } catch (HttpFetcherException e) {
                logError("Network error while retrieving my segments: " + e.getLocalizedMessage());
                if(mRetryOnFail) {
                    try {
                        logError("Retrying...");
                        Thread.sleep(backoffCounter.getNextRetryTime());
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return SplitTaskExecutionInfo.error(SplitTaskType.MY_SEGMENTS_SYNC);
                    }
                } else {
                    return SplitTaskExecutionInfo.error(SplitTaskType.MY_SEGMENTS_SYNC);
                }
            } catch (Exception e) {
                logError("Unknown error while retrieving my segments: " + e.getLocalizedMessage());
                return SplitTaskExecutionInfo.error(SplitTaskType.MY_SEGMENTS_SYNC);
            }
        }
        Logger.d("My Segments have been updated");
        return SplitTaskExecutionInfo.success(SplitTaskType.MY_SEGMENTS_SYNC);
    }

    private void logError(String message) {
        Logger.e("Error while executing my segments sync task: " + message);
    }

    private List<String> getNameList(List<MySegment> mySegments) {
        List<String> nameList = new ArrayList<String>();
        for(MySegment segment : mySegments) {
            nameList.add(segment.name);
        }
        return nameList;
    }
}
