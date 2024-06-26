package io.split.android.client.service.synchronizer;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.common.InBytesSizable;
import io.split.android.client.storage.common.StoragePusher;

public class RecorderSyncHelperImpl<T extends InBytesSizable> implements RecorderSyncHelper<T> {

    private final StoragePusher<T> mStorage;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final AtomicInteger mPushedCount;
    private final AtomicLong mTotalPushedSizeInBytes;
    private final int mMaxQueueSize;
    private final long mMaxQueueSizeInBytes;
    private final SplitTaskType mTaskType;
    private WeakReference<SplitTaskExecutionListener> mTaskExecutionListener;

    public RecorderSyncHelperImpl(SplitTaskType taskType,
                                  StoragePusher<T> storage,
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
        mTaskExecutionListener = new WeakReference<>(null);
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

        if (mTaskExecutionListener.get() != null) {
            mTaskExecutionListener.get().taskExecuted(taskInfo);
        }
    }

    @Override
    public void addListener(SplitTaskExecutionListener listener) {
        mTaskExecutionListener = new WeakReference<>(listener);
    }

    @Override
    public void removeListener(SplitTaskExecutionListener listener) {
        mTaskExecutionListener = new WeakReference<>(null);
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
