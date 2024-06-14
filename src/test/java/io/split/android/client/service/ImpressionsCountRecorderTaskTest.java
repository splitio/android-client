package io.split.android.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.service.impressions.ImpressionsCount;
import io.split.android.client.service.impressions.ImpressionsCountPerFeature;
import io.split.android.client.service.impressions.ImpressionsCountRecorderTask;
import io.split.android.client.storage.impressions.PersistentImpressionsCountStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class ImpressionsCountRecorderTaskTest {

    final static SplitTaskType TASK_TYPE = SplitTaskType.IMPRESSIONS_COUNT_RECORDER;
    final static int DEFAULT_POP_CONFIG = ServiceConstants.DEFAULT_IMPRESSION_COUNT_ROWS_POP;

    HttpRecorder<ImpressionsCount> mImpressionsRecorder;
    PersistentImpressionsCountStorage mPersistentImpressionsStorage;
    TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    List<ImpressionsCountPerFeature> mDefaultImpressions;
    ImpressionsCount mDefaultParams;

    @Before
    public void setup() {
        mDefaultImpressions = createImpressions();
        mDefaultParams = new ImpressionsCount(mDefaultImpressions);
        mImpressionsRecorder = (HttpRecorder<ImpressionsCount>) Mockito.mock(HttpRecorder.class);
        mPersistentImpressionsStorage = Mockito.mock(PersistentImpressionsCountStorage.class);
        mTelemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
    }

    @Test
    public void correctExecution() throws HttpRecorderException {

        ArgumentCaptor<SplitTaskExecutionInfo> taskInfoCaptor = ArgumentCaptor.forClass(SplitTaskExecutionInfo.class);

        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultImpressions)
                .thenReturn(mDefaultImpressions)
                .thenReturn(new ArrayList<>());

        ImpressionsCountRecorderTask task = new ImpressionsCountRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mTelemetryRuntimeProducer);

        SplitTaskExecutionInfo result = task.execute();

        verify(mImpressionsRecorder, times(2)).execute(new ImpressionsCount(mDefaultImpressions));
        verify(mPersistentImpressionsStorage, times(3)).pop(DEFAULT_POP_CONFIG);
        verify(mPersistentImpressionsStorage, times(2)).delete(any());
        verify(mPersistentImpressionsStorage, never()).setActive(any());

        Assert.assertEquals(TASK_TYPE, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void throwingException() throws HttpRecorderException {

        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultImpressions)
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "")).when(mImpressionsRecorder).execute(mDefaultParams);

        ImpressionsCountRecorderTask task = new ImpressionsCountRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mTelemetryRuntimeProducer);

        SplitTaskExecutionInfo result = task.execute();

        verify(mImpressionsRecorder, times(1)).execute(mDefaultParams);
        verify(mPersistentImpressionsStorage, times(2)).pop(DEFAULT_POP_CONFIG);
        verify(mPersistentImpressionsStorage, never()).delete(any());
        verify(mPersistentImpressionsStorage, times(1)).setActive(any());

        Assert.assertEquals(TASK_TYPE, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
        Assert.assertNull(result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    @Test
    public void emptyImpressions() throws HttpRecorderException {

        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "")).when(mImpressionsRecorder).execute(mDefaultParams);

        ImpressionsCountRecorderTask task = new ImpressionsCountRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mTelemetryRuntimeProducer);

        SplitTaskExecutionInfo result = task.execute();

        verify(mImpressionsRecorder, times(0)).execute(mDefaultParams);
        verify(mPersistentImpressionsStorage, times(1)).pop(DEFAULT_POP_CONFIG);
        verify(mPersistentImpressionsStorage, never()).delete(any());
        verify(mPersistentImpressionsStorage, never()).setActive(any());

        Assert.assertEquals(TASK_TYPE, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertNull(result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    @Test
    public void errorIsRecordedInTelemetry() throws HttpRecorderException {

        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultImpressions)
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "", 500)).when(mImpressionsRecorder).execute(mDefaultParams);

        ImpressionsCountRecorderTask task = new ImpressionsCountRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mTelemetryRuntimeProducer);

        task.execute();

        verify(mTelemetryRuntimeProducer).recordSyncError(OperationType.IMPRESSIONS_COUNT, 500);
    }

    @Test
    public void latencyIsRecordedInTelemetry() {
        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultImpressions)
                .thenReturn(mDefaultImpressions)
                .thenReturn(new ArrayList<>());

        ImpressionsCountRecorderTask task = new ImpressionsCountRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mTelemetryRuntimeProducer);

        task.execute();

        verify(mTelemetryRuntimeProducer, atLeastOnce()).recordSyncLatency(eq(OperationType.IMPRESSIONS_COUNT), anyLong());
    }

    @Test
    public void successIsRecordedInTelemetry() {
        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultImpressions)
                .thenReturn(mDefaultImpressions)
                .thenReturn(new ArrayList<>());

        ImpressionsCountRecorderTask task = new ImpressionsCountRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mTelemetryRuntimeProducer);

        task.execute();

        verify(mTelemetryRuntimeProducer, atLeastOnce()).recordSuccessfulSync(eq(OperationType.IMPRESSIONS_COUNT), longThat(arg -> arg > 0));
    }

    @Test
    public void statusCode9009InHttpExceptionReturnsDoNotRetry() throws HttpRecorderException {
        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultImpressions)
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "", 9009)).when(mImpressionsRecorder).execute(mDefaultParams);

        ImpressionsCountRecorderTask task = new ImpressionsCountRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mTelemetryRuntimeProducer);

        SplitTaskExecutionInfo result = task.execute();

        Assert.assertEquals(TASK_TYPE, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
        Assert.assertEquals(true, result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    @Test
    public void nullStatusCodeInExceptionReturnsNullDoNotRetry() throws HttpRecorderException {
        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultImpressions)
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "")).when(mImpressionsRecorder).execute(mDefaultParams);

        ImpressionsCountRecorderTask task = new ImpressionsCountRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mTelemetryRuntimeProducer);

        SplitTaskExecutionInfo result = task.execute();

        Assert.assertEquals(TASK_TYPE, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
        Assert.assertNull(result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    @Test
    public void statusCode9009InHttpExceptionBreaksLoop() throws HttpRecorderException {
        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultImpressions)
                .thenReturn(mDefaultImpressions)
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "", 9009)).when(mImpressionsRecorder).execute(mDefaultParams);

        ImpressionsCountRecorderTask task = new ImpressionsCountRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mTelemetryRuntimeProducer);

        SplitTaskExecutionInfo result = task.execute();

        verify(mImpressionsRecorder, times(1)).execute(mDefaultParams);
        Assert.assertEquals(TASK_TYPE, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
        Assert.assertEquals(true, result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    @After
    public void tearDown() {
        reset(mImpressionsRecorder);
        reset(mPersistentImpressionsStorage);
    }

    private List<ImpressionsCountPerFeature> createImpressions() {
        List<ImpressionsCountPerFeature> impressions = new ArrayList<>();
        for (int i = 0; i < DEFAULT_POP_CONFIG; i++) {
            ImpressionsCountPerFeature count = newCount("feature_" + i, i + 100, i);
            impressions.add(count);
        }
        return impressions;
    }

    private ImpressionsCountPerFeature newCount(String feature, long timeframe, int count) {
        return new ImpressionsCountPerFeature(feature, 100, 5);
    }
}
