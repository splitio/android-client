package io.split.android.client.service.impressions.unique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.storage.impressions.PersistentImpressionsUniqueStorage;

public class UniqueKeysRecorderTaskTest {

    private UniqueKeysRecorderTask mRecorderTask;

    @Mock
    private HttpRecorder<MTK> mRecorder;

    @Mock
    private PersistentImpressionsUniqueStorage mStorage;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mRecorderTask = new UniqueKeysRecorderTask(
                mRecorder,
                mStorage,
                new UniqueKeysRecorderTaskConfig(10, 128000)
        );
    }

    @Test
    public void correctExecution() throws HttpRecorderException {
        List<UniqueKey> keys = createKeys(0);
        List<UniqueKey> keys1 = createKeys(10);

        when(mStorage.pop(10))
                .thenReturn(keys)
                .thenReturn(keys1)
                .thenReturn(new ArrayList<>());

        SplitTaskExecutionInfo executionInfo = mRecorderTask.execute();

        verify(mRecorder).execute(argThat(argument -> argument.getKeys().size() == keys.size() && argument.getKeys().containsAll(keys)));
        verify(mRecorder).execute(argThat(argument -> argument.getKeys().size() == keys1.size() && argument.getKeys().containsAll(keys1)));
        verify(mStorage, times(3)).pop(10);
        verify(mStorage, times(2)).delete(any());
        verify(mStorage, never()).setActive(any());

        assertEquals(SplitTaskExecutionStatus.SUCCESS, executionInfo.getStatus());
        assertEquals(SplitTaskType.UNIQUE_KEYS_RECORDER_TASK, executionInfo.getTaskType());
        Assert.assertNull(executionInfo.getIntegerValue(SplitTaskExecutionInfo.NON_SENT_RECORDS));
        Assert.assertNull(executionInfo.getLongValue(SplitTaskExecutionInfo.NON_SENT_BYTES));
    }

    @Test
    public void featuresForSameKeyAreMerged() throws HttpRecorderException {
        List<UniqueKey> keys = new ArrayList<>();
        Set<String> set1 = new HashSet<>();
        set1.add("f1");
        set1.add("f2");
        set1.add("f3");

        Set<String> set2 = new HashSet<>();
        set2.add("f2");
        set2.add("f3");
        set2.add("f4");
        set2.add("f5");
        keys.add(new UniqueKey("key1", set1));
        keys.add(new UniqueKey("key1", set2));

        Set<String> expectedSet = new HashSet<>();
        expectedSet.add("f1");
        expectedSet.add("f2");
        expectedSet.add("f3");
        expectedSet.add("f4");
        expectedSet.add("f5");

        when(mStorage.pop(10))
                .thenReturn(keys);

        SplitTaskExecutionInfo executionInfo = mRecorderTask.execute();

        ArgumentCaptor<MTK> argumentCaptor = ArgumentCaptor.forClass(MTK.class);

        verify(mRecorder).execute(argumentCaptor.capture());
        verify(mStorage, never()).setActive(any());

        List<UniqueKey> uniqueKeys = argumentCaptor.getValue().getKeys();

        assertEquals(1, uniqueKeys.size());
        assertEquals(uniqueKeys.get(0).getFeatures(), expectedSet);
        assertEquals("key1", uniqueKeys.get(0).getKey());

        assertEquals(SplitTaskExecutionStatus.SUCCESS, executionInfo.getStatus());
        assertEquals(SplitTaskType.UNIQUE_KEYS_RECORDER_TASK, executionInfo.getTaskType());
        Assert.assertNull(executionInfo.getIntegerValue(SplitTaskExecutionInfo.NON_SENT_RECORDS));
        Assert.assertNull(executionInfo.getLongValue(SplitTaskExecutionInfo.NON_SENT_BYTES));
    }

    @Test
    public void throwingException() throws HttpRecorderException {
        when(mStorage.pop(10))
                .thenReturn(createKeys(0))
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "")).when(mRecorder).execute(any());

        SplitTaskExecutionInfo executionInfo = mRecorderTask.execute();

        verify(mRecorder, times(1)).execute(any());
        verify(mStorage, times(2)).pop(10);
        verify(mStorage, never()).delete(any());
        verify(mStorage, times(1)).setActive(any());

        Assert.assertEquals(SplitTaskType.UNIQUE_KEYS_RECORDER_TASK, executionInfo.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, executionInfo.getStatus());
        Assert.assertEquals(10, executionInfo.getIntegerValue(SplitTaskExecutionInfo.NON_SENT_RECORDS).intValue());
        Assert.assertEquals(1280000, executionInfo.getLongValue(SplitTaskExecutionInfo.NON_SENT_BYTES).longValue());
    }

    @Test
    public void emptyImpressions() throws HttpRecorderException {
        when(mStorage.pop(10))
                .thenReturn(new ArrayList<>());

        SplitTaskExecutionInfo result = mRecorderTask.execute();

        verify(mRecorder, times(0)).execute(any());
        verify(mStorage, times(1)).pop(10);
        verify(mStorage, never()).delete(any());
        verify(mStorage, never()).setActive(any());

        Assert.assertEquals(SplitTaskType.UNIQUE_KEYS_RECORDER_TASK, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertNull(result.getIntegerValue(SplitTaskExecutionInfo.NON_SENT_RECORDS));
        Assert.assertNull(result.getLongValue(SplitTaskExecutionInfo.NON_SENT_BYTES));
    }

    private List<UniqueKey> createKeys(int initialIndex) {
        List<UniqueKey> keys = new ArrayList<>();
        for (int i = initialIndex; i < initialIndex + 10; i++) {
            UniqueKey impression = new UniqueKey("key" + i, new HashSet<>(Arrays.asList("feature1", "feature2")));
            keys.add(impression);
        }

        return keys;
    }
}
