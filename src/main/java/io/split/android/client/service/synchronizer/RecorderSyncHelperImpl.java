package io.split.android.client.service.synchronizer;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.InBytesSizable;
import io.split.android.client.storage.StoragePusher;

import static com.google.common.base.Preconditions.checkNotNull;

class RecorderSyncHelperImpl<T extends InBytesSizable> implements RecorderSyncHelper<T> {

    private final StoragePusher mStorage;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private AtomicInteger mPushedCount;
    private AtomicLong mTotalPushedSizeInBytes;
    private final int mMaxQueueSize;
    private final long mMaxQueueSizeInBytes;
    private final SplitTaskType mTaskType;

    public RecorderSyncHelperImpl(SplitTaskType taskType,
                                  StoragePusher storage,
                                  int maxQueueSize,
                                  long maxQueueSizeInBytes,
                                  SplitTaskExecutor splitTaskExecutor) {
        mTaskType = checkNotNull(taskType);
        mStorage = checkNotNull(storage);
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mPushedCount = new AtomicInteger(0);
        mTotalPushedSizeInBytes = new AtomicLong(0);
        mMaxQueueSize = maxQueueSize;
        mMaxQueueSizeInBytes = maxQueueSizeInBytes;
    }

    @Override
    public boolean pushAndCheckIfFlushNeeded(T entity) {
        pushAsync(entity);
        int pushedEventCount = mPushedCount.addAndGet(1);
        long totalEventsSizeInBytes = mTotalPushedSizeInBytes.addAndGet(entity.getSizeInBytes());
        if (pushedEventCount > mMaxQueueSize ||
                totalEventsSizeInBytes >= mMaxQueueSizeInBytes) {
            mPushedCount.set(0);
            mTotalPushedSizeInBytes.set(0);
            return true;
        }
        return false;
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        if (mTaskType.equals(taskInfo.getTaskType()) &&
                taskInfo.getStatus().equals(SplitTaskExecutionStatus.ERROR)) {
            mPushedCount.addAndGet(taskInfo.getIntegerValue(
                    SplitTaskExecutionInfo.NON_SENT_RECORDS));
            mTotalPushedSizeInBytes.addAndGet(taskInfo.getLongValue(
                    SplitTaskExecutionInfo.NON_SENT_BYTES));
        }
    }

    private void pushAsync(T entity) {
        mSplitTaskExecutor.submit(new SplitTask() {
            @NonNull
            @Override
            public SplitTaskExecutionInfo execute() {
                mStorage.push(entity);
                return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
            }
        }, null);
    }
}
