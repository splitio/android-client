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
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.service.impressions.ImpressionsRecorderTask;
import io.split.android.client.service.impressions.ImpressionsRecorderTaskConfig;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImpressionsRecorderTaskTest {

    final static SplitTaskType TASK_TYPE = SplitTaskType.IMPRESSIONS_RECORDER;
    final static int DEFAULT_POP_CONFIG = 100;

    HttpRecorder<List<KeyImpression>> mImpressionsRecorder;
    PersistentImpressionsStorage mPersistentImpressionsStorage;
    SplitTaskExecutionListener mTaskExecutionListener;


    List<KeyImpression> mDefaultParams = new ArrayList<>();
    ImpressionsRecorderTaskConfig mDefaultConfig
            = new ImpressionsRecorderTaskConfig(DEFAULT_POP_CONFIG, 512L);

    @Before
    public void setup() {
        mDefaultParams = createImpressions();
        mImpressionsRecorder = (HttpRecorder<List<KeyImpression>>) Mockito.mock(HttpRecorder.class);
        mPersistentImpressionsStorage = Mockito.mock(PersistentImpressionsStorage.class);
        mTaskExecutionListener = Mockito.mock(SplitTaskExecutionListener.class);
    }

    @Test
    public void correctExecution() throws HttpRecorderException {

        ArgumentCaptor<SplitTaskExecutionInfo> taskInfoCaptor = ArgumentCaptor.forClass(SplitTaskExecutionInfo.class);

        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(mDefaultParams)
                .thenReturn(new ArrayList<>());

        ImpressionsRecorderTask task = new ImpressionsRecorderTask(
                SplitTaskType.IMPRESSIONS_RECORDER,
                mTaskExecutionListener,
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mDefaultConfig);

        task.execute();

        verify(mImpressionsRecorder, times(2)).execute(mDefaultParams);
        verify(mPersistentImpressionsStorage, times(3)).pop(DEFAULT_POP_CONFIG);
        verify(mTaskExecutionListener, times(1)).taskExecuted(taskInfoCaptor.capture());

        SplitTaskExecutionInfo result = taskInfoCaptor.getValue();
        Assert.assertEquals(TASK_TYPE, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertEquals(0, result.getNonSentRecords());
        Assert.assertEquals(0, result.getNonSentBytes());
    }

    @Test
    public void throwingException() throws HttpRecorderException {

        ArgumentCaptor<SplitTaskExecutionInfo> taskInfoCaptor = ArgumentCaptor.forClass(SplitTaskExecutionInfo.class);

        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("","")).when(mImpressionsRecorder).execute(mDefaultParams);

        ImpressionsRecorderTask task = new ImpressionsRecorderTask(
                SplitTaskType.IMPRESSIONS_RECORDER,
                mTaskExecutionListener,
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mDefaultConfig);

        task.execute();

        verify(mImpressionsRecorder, times(1)).execute(mDefaultParams);
        verify(mPersistentImpressionsStorage, times(2)).pop(DEFAULT_POP_CONFIG);
        verify(mTaskExecutionListener, times(1)).taskExecuted(taskInfoCaptor.capture());

        SplitTaskExecutionInfo result = taskInfoCaptor.getValue();
        Assert.assertEquals(TASK_TYPE, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
        Assert.assertEquals(100, result.getNonSentRecords());
        Assert.assertEquals(51200, result.getNonSentBytes());
    }

    @Test
    public void emptyImpressions() throws HttpRecorderException {

        ArgumentCaptor<SplitTaskExecutionInfo> taskInfoCaptor = ArgumentCaptor.forClass(SplitTaskExecutionInfo.class);

        when(mPersistentImpressionsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("","")).when(mImpressionsRecorder).execute(mDefaultParams);

        ImpressionsRecorderTask task = new ImpressionsRecorderTask(
                SplitTaskType.IMPRESSIONS_RECORDER,
                mTaskExecutionListener,
                mImpressionsRecorder,
                mPersistentImpressionsStorage,
                mDefaultConfig);

        task.execute();

        verify(mImpressionsRecorder, times(0)).execute(mDefaultParams);
        verify(mPersistentImpressionsStorage, times(1)).pop(DEFAULT_POP_CONFIG);
        verify(mTaskExecutionListener, times(1)).taskExecuted(taskInfoCaptor.capture());

        SplitTaskExecutionInfo result = taskInfoCaptor.getValue();
        Assert.assertEquals(TASK_TYPE, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertEquals(0, result.getNonSentRecords());
        Assert.assertEquals(0, result.getNonSentBytes());
    }

    @After
    public void tearDown() {
        reset(mImpressionsRecorder);
        reset(mPersistentImpressionsStorage);
    }

    private List<KeyImpression> createImpressions() {
        List<KeyImpression> impressions = new ArrayList<>();
        for(int i=0; i<DEFAULT_POP_CONFIG; i++) {
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
