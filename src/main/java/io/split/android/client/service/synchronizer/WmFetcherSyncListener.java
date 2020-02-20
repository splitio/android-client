package io.split.android.client.service.synchronizer;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;

import static com.google.common.base.Preconditions.checkNotNull;

class WmFetcherSyncListener implements SplitTaskExecutionListener {

    WeakReference<WmFetcherSyncListenerDelegate> mDelegate;

    public WmFetcherSyncListener(WmFetcherSyncListenerDelegate delegate) {
        mDelegate = new WeakReference<>(delegate);
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        WmFetcherSyncListenerDelegate delegate = mDelegate.get();
        if(delegate != null) {
            switch(taskInfo.getTaskType()) {
                case SPLITS_SYNC:
                    delegate.splitsUpdatedInBackground();
                    break;
                case MY_SEGMENTS_SYNC:
                    delegate.mySegmentsUpdatedInBackground();
                    break;
            }
        }
    }
}
