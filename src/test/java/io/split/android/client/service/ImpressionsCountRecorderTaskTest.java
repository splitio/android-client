package io.split.android.client.service;

import org.junit.After;
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
import io.split.android.client.service.impressions.ImpressionsCount;
import io.split.android.client.service.impressions.ImpressionsCountPerFeature;
import io.split.android.client.service.impressions.ImpressionsCountRecorderTask;
import io.split.android.client.service.impressions.ImpressionsRecorderTask;
import io.split.android.client.service.impressions.ImpressionsRecorderTaskConfig;
import io.split.android.client.storage.impressions.PersistentImpressionsCountStorage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImpressionsCountRecorderTaskTest {

    final static SplitTaskType TASK_TYPE = SplitTaskType.IMPRESSIONS_COUNT_RECORDER;
    final static int DEFAULT_POP_CONFIG = ServiceConstants.DEFAULT_IMPRESSION_COUNT_ROWS_POP;

    HttpRecorder<ImpressionsCount> mImpressionsRecorder;
    PersistentImpressionsCountStorage mPersistentImpressionsStorage;

    List<ImpressionsCountPerFeature> mDefaultImpressions;
    ImpressionsCount mDefaultParams;
    ImpressionsRecorderTaskConfig mDefaultConfig
            = new ImpressionsRecorderTaskConfig(DEFAULT_POP_CONFIG, 512L);

    @Before
    public void setup() {
        mDefaultImpressions = createImpressions();
        mDefaultParams = new ImpressionsCount(mDefaultImpressions);
        mImpressionsRecorder = (HttpRecorder<ImpressionsCount>) Mockito.mock(HttpRecorder.class);
        mPersistentImpressionsStorage = Mockito.mock(PersistentImpressionsCountStorage.class);
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
                mPersistentImpressionsStorage);

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
                mPersistentImpressionsStorage);

        SplitTaskExecutionInfo result = task.execute();

        verify(mImpressionsRecorder, times(1)).execute(mDefaultParams);
        verify(mPersistentImpressionsStorage, times(2)).pop(DEFAULT_POP_CONFIG);
        verify(mPersistentImpressionsStorage, never()).delete(any());
        verify(mPersistentImpressionsStorage, times(1)).setActive(any());

        Assert.assertEquals(TASK_TYPE, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void emptyImpressions() throws HttpRecorderException {

        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "")).when(mImpressionsRecorder).execute(mDefaultParams);

        ImpressionsCountRecorderTask task = new ImpressionsCountRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage);

        SplitTaskExecutionInfo result = task.execute();

        verify(mImpressionsRecorder, times(0)).execute(mDefaultParams);
        verify(mPersistentImpressionsStorage, times(1)).pop(DEFAULT_POP_CONFIG);
        verify(mPersistentImpressionsStorage, never()).delete(any());
        verify(mPersistentImpressionsStorage, never()).setActive(any());

        Assert.assertEquals(TASK_TYPE, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void addHttpStatusWhenHttpError() throws HttpRecorderException {

        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultImpressions)
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "", 500)).when(mImpressionsRecorder).execute(mDefaultParams);

        ImpressionsCountRecorderTask task = new ImpressionsCountRecorderTask(
                mImpressionsRecorder,
                mPersistentImpressionsStorage);

        SplitTaskExecutionInfo result = task.execute();

        Assert.assertEquals(500, result.getIntegerValue("HTTP_STATUS").intValue());
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
