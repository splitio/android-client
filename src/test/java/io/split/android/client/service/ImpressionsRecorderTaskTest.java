package io.split.android.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.service.impressions.ImpressionsRecorderTask;
import io.split.android.client.service.impressions.ImpressionsRecorderTaskConfig;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class ImpressionsRecorderTaskTest {

    final static SplitTaskType TASK_TYPE = SplitTaskType.IMPRESSIONS_RECORDER;
    final static int DEFAULT_POP_CONFIG = 100;

    HttpRecorder<List<KeyImpression>> mImpressionsRecorder;
    PersistentImpressionsStorage mPersistentImpressionsStorage;
    TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    List<KeyImpression> mDefaultParams = new ArrayList<>();
    ImpressionsRecorderTaskConfig mDefaultConfig
            = new ImpressionsRecorderTaskConfig(DEFAULT_POP_CONFIG, 512L, true);

    @Before
    public void setup() {
        mDefaultParams = createImpressions();
        mImpressionsRecorder = (HttpRecorder<List<KeyImpression>>) Mockito.mock(HttpRecorder.class);
        mPersistentImpressionsStorage = Mockito.mock(PersistentImpressionsStorage.class);
        mTelemetryRuntimeProducer = mock(TelemetryRuntimeProducer.class);
    }

    @Test
    public void correctExecution() throws HttpRecorderException {

        ArgumentCaptor<SplitTaskExecutionInfo> taskInfoCaptor = ArgumentCaptor.forClass(SplitTaskExecutionInfo.class);

        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(mDefaultParams)
                .thenReturn(new ArrayList<>());

        ImpressionsRecorderTask task = new ImpressionsRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mDefaultConfig,
                mTelemetryRuntimeProducer);

        SplitTaskExecutionInfo result = task.execute();

        verify(mImpressionsRecorder, times(2)).execute(mDefaultParams);
        verify(mPersistentImpressionsStorage, times(3)).pop(DEFAULT_POP_CONFIG);
        verify(mPersistentImpressionsStorage, times(2)).delete(any());
        verify(mPersistentImpressionsStorage, never()).setActive(any());

        Assert.assertEquals(TASK_TYPE, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertNull(result.getIntegerValue(SplitTaskExecutionInfo.NON_SENT_RECORDS));
        Assert.assertNull(result.getLongValue(SplitTaskExecutionInfo.NON_SENT_BYTES));
    }

    @Test
    public void throwingException() throws HttpRecorderException {

        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "")).when(mImpressionsRecorder).execute(mDefaultParams);

        ImpressionsRecorderTask task = new ImpressionsRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mDefaultConfig,
                mTelemetryRuntimeProducer);

        SplitTaskExecutionInfo result = task.execute();

        verify(mImpressionsRecorder, times(1)).execute(mDefaultParams);
        verify(mPersistentImpressionsStorage, times(2)).pop(DEFAULT_POP_CONFIG);
        verify(mPersistentImpressionsStorage, never()).delete(any());
        verify(mPersistentImpressionsStorage, times(1)).setActive(any());

        Assert.assertEquals(TASK_TYPE, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
        Assert.assertEquals(100, result.getIntegerValue(SplitTaskExecutionInfo.NON_SENT_RECORDS).intValue());
        Assert.assertEquals(51200, result.getLongValue(SplitTaskExecutionInfo.NON_SENT_BYTES).longValue());
    }

    @Test
    public void emptyImpressions() throws HttpRecorderException {

        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "")).when(mImpressionsRecorder).execute(mDefaultParams);

        ImpressionsRecorderTask task = new ImpressionsRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mDefaultConfig,
                mTelemetryRuntimeProducer);

        SplitTaskExecutionInfo result = task.execute();

        verify(mImpressionsRecorder, times(0)).execute(mDefaultParams);
        verify(mPersistentImpressionsStorage, times(1)).pop(DEFAULT_POP_CONFIG);
        verify(mPersistentImpressionsStorage, never()).delete(any());
        verify(mPersistentImpressionsStorage, never()).setActive(any());

        Assert.assertEquals(TASK_TYPE, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertNull(result.getIntegerValue(SplitTaskExecutionInfo.NON_SENT_RECORDS));
        Assert.assertNull(result.getLongValue(SplitTaskExecutionInfo.NON_SENT_BYTES));
    }

    @Test
    public void errorIsRecordedInTelemetry() throws HttpRecorderException {
        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "", 500)).when(mImpressionsRecorder).execute(mDefaultParams);

        ImpressionsRecorderTask task = new ImpressionsRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mDefaultConfig,
                mTelemetryRuntimeProducer);

        task.execute();

        verify(mTelemetryRuntimeProducer).recordSyncError(OperationType.IMPRESSIONS, 500);
    }

    @Test
    public void latencyIsRecordedInTelemetry() {
        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(mDefaultParams)
                .thenReturn(new ArrayList<>());

        ImpressionsRecorderTask task = new ImpressionsRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mDefaultConfig,
                mTelemetryRuntimeProducer);

        task.execute();

        verify(mTelemetryRuntimeProducer, atLeastOnce()).recordSyncLatency(eq(OperationType.IMPRESSIONS), anyLong());
    }

    @Test
    public void successIsRecordedInTelemetry() {
        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(mDefaultParams)
                .thenReturn(new ArrayList<>());

        ImpressionsRecorderTask task = new ImpressionsRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mDefaultConfig,
                mTelemetryRuntimeProducer);

        task.execute();

        verify(mTelemetryRuntimeProducer, atLeastOnce()).recordSuccessfulSync(eq(OperationType.IMPRESSIONS), longThat(arg -> arg > 0));
    }

    private List<KeyImpression> createImpressions() {
        List<KeyImpression> impressions = new ArrayList<>();
        for (int i = 0; i < DEFAULT_POP_CONFIG; i++) {
            KeyImpression impression = newImpression("feature_" + ((i % 2) == 0 ? 1 : 2), "impression_" + i);
            impressions.add(impression);
        }
        return impressions;
    }

    private KeyImpression newImpression(String feature, String key) {
        KeyImpression impression = new KeyImpression();
        impression.changeNumber = 100L;
        impression.feature = feature;
        impression.keyName = key;
        impression.treatment = "on";
        impression.time = 100;
        return impression;
    }
}
