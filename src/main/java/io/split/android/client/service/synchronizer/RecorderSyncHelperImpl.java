package io.split.android.client.service.synchronizer;

import androidx.annotation.NonNull;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.storage.InBytesSizable;
import io.split.android.client.storage.StoragePusher;

import static com.google.common.base.Preconditions.checkNotNull;

public class RecorderSyncHelperImpl<T extends InBytesSizable> implements RecorderSyncHelper<T> {

    private final StoragePusher mStorage;
    private AtomicInteger mPushedCount;
    private AtomicLong mTotalPushedSizeInBytes;
    private final int mMaxQueueSize;

    public RecorderSyncHelperImpl(StoragePusher storage,
                                  int maxQueueSize) {
        this.mStorage = checkNotNull(storage);
        mPushedCount = new AtomicInteger(0);
        mTotalPushedSizeInBytes = new AtomicLong(0);
        mMaxQueueSize = maxQueueSize;
    }

    @Override
    public boolean pushAndCheckIfFlushNeeded(T entity) {
        pushAsync(entity);
        int pushedEventCount = mPushedCount.addAndGet(1);
        long totalEventsSizeInBytes = mTotalPushedSizeInBytes.addAndGet(entity.getSizeInBytes());
        if (pushedEventCount > mMaxQueueSize ||
                totalEventsSizeInBytes >= ServiceConstants.MAX_EVENTS_SIZE_BYTES) {
            mPushedCount.set(0);
            mTotalPushedSizeInBytes.set(0);
            return true;
        }
        return false;
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        if (taskInfo.getStatus() == SplitTaskExecutionStatus.ERROR) {
            mPushedCount.addAndGet(taskInfo.getNonSentRecords());
            mTotalPushedSizeInBytes.addAndGet(taskInfo.getNonSentBytes());
        }
    }

    private void pushAsync(T entity) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mStorage.push(entity);
            }
        }).start();
    }
}
