package io.split.android.client.service.mysegments;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class MySegmentsSyncTask implements SplitTask {

    private final MySegmentsFetcherV2 mMySegmentsFetcher;
    private final MySegmentsStorage mMySegmentsStorage;

    public MySegmentsSyncTask(MySegmentsFetcherV2 mySegmentsFetcher, MySegmentsStorage mySegmentsStorage) {
        checkNotNull(mySegmentsFetcher);
        checkNotNull(mySegmentsStorage);

        mMySegmentsFetcher = mySegmentsFetcher;
        mMySegmentsStorage = mySegmentsStorage;
    }

    @Override
    public void execute() {
        try {
            mMySegmentsStorage.set(getNameList(mMySegmentsFetcher.execute()));
        } catch (IllegalStateException e) {
            logError(e.getLocalizedMessage());
        } catch (Exception e) {
            logError("unexpected " + e.getLocalizedMessage());
        }
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
