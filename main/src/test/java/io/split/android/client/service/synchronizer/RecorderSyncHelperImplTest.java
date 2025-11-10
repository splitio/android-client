package io.split.android.client.service.synchronizer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.common.StoragePusher;

public class RecorderSyncHelperImplTest {

    private StoragePusher<KeyImpression> mStoragePusher;
    private SplitTaskExecutor mTaskExecutor;

    @Test
    public void taskExecutionCallsListenerWhenAdded() {
        mStoragePusher = mock(StoragePusher.class);
        mTaskExecutor = mock(SplitTaskExecutor.class);

        RecorderSyncHelperImpl<KeyImpression> syncHelper = new RecorderSyncHelperImpl<>(
                SplitTaskType.IMPRESSIONS_RECORDER,
                mStoragePusher,
                1,
                1,
                mTaskExecutor);

        SplitTaskExecutionListener listener = mock(SplitTaskExecutionListener.class);
        syncHelper.addListener(listener);

        SplitTaskExecutionInfo executionInfo = SplitTaskExecutionInfo.success(SplitTaskType.IMPRESSIONS_RECORDER);
        syncHelper.taskExecuted(executionInfo);

        verify(listener).taskExecuted(executionInfo);
    }

    @Test
    public void listenerIsNotCalledOnceRemoved() {
        mStoragePusher = mock(StoragePusher.class);
        mTaskExecutor = mock(SplitTaskExecutor.class);

        RecorderSyncHelperImpl<KeyImpression> syncHelper = new RecorderSyncHelperImpl<>(
                SplitTaskType.IMPRESSIONS_RECORDER,
                mStoragePusher,
                1,
                1,
                mTaskExecutor);

        SplitTaskExecutionListener listener = mock(SplitTaskExecutionListener.class);
        SplitTaskExecutionListener listener2 = mock(SplitTaskExecutionListener.class);
        SplitTaskExecutionInfo executionInfo = SplitTaskExecutionInfo.success(SplitTaskType.IMPRESSIONS_RECORDER);

        syncHelper.addListener(listener);
        syncHelper.addListener(listener2);
        syncHelper.taskExecuted(executionInfo);
        syncHelper.removeListener(listener);
        syncHelper.taskExecuted(executionInfo);

        verify(listener, times(1)).taskExecuted(executionInfo);
        verify(listener2, times(2)).taskExecuted(executionInfo);
    }

    @Test
    public void multipleListenersCanBeAdded() {
        mStoragePusher = mock(StoragePusher.class);
        mTaskExecutor = mock(SplitTaskExecutor.class);

        RecorderSyncHelperImpl<KeyImpression> syncHelper = new RecorderSyncHelperImpl<>(
                SplitTaskType.IMPRESSIONS_RECORDER,
                mStoragePusher,
                1,
                1,
                mTaskExecutor);

        SplitTaskExecutionListener listener1 = mock(SplitTaskExecutionListener.class);
        SplitTaskExecutionListener listener2 = mock(SplitTaskExecutionListener.class);
        SplitTaskExecutionInfo executionInfo = SplitTaskExecutionInfo.success(SplitTaskType.IMPRESSIONS_RECORDER);

        syncHelper.addListener(listener1);
        syncHelper.addListener(listener2);
        syncHelper.taskExecuted(executionInfo);

        verify(listener1).taskExecuted(executionInfo);
        verify(listener2).taskExecuted(executionInfo);
    }
}
