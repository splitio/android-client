package io.split.android.client.service.impressions.unique;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.storage.impressions.PersistentImpressionsUniqueStorage;

public class SaveUniqueImpressionsTaskTest {

    private SaveUniqueImpressionsTask mSaveUniqueImpressionsTask;

    @Mock
    private PersistentImpressionsUniqueStorage mStorage;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void nullInputDoesNotInteractWithStorage() {
        mSaveUniqueImpressionsTask = new SaveUniqueImpressionsTask(mStorage, null);

        SplitTaskExecutionInfo executionInfo = mSaveUniqueImpressionsTask.execute();

        verifyNoInteractions(mStorage);

        assertEquals(SplitTaskExecutionStatus.SUCCESS, executionInfo.getStatus());
    }

    @Test
    public void emptyInputDoesNotInteractWithStorage() {
        mSaveUniqueImpressionsTask = new SaveUniqueImpressionsTask(mStorage, Collections.emptyMap());

        SplitTaskExecutionInfo executionInfo = mSaveUniqueImpressionsTask.execute();

        verifyNoInteractions(mStorage);

        assertEquals(SplitTaskExecutionStatus.SUCCESS, executionInfo.getStatus());
    }

    @Test
    public void taskIsPerformedCorrectly() {
        Map<String, Set<String>> inputMap = new HashMap<>();
        inputMap.put("key1", getSet("value1", "value2", "value3"));
        inputMap.put("key2", getSet("value1", "value2", "value7", "value8"));
        inputMap.put("key3", getSet("value1", "value2", "value7", "value8"));

        mSaveUniqueImpressionsTask = new SaveUniqueImpressionsTask(mStorage, inputMap);
        SplitTaskExecutionInfo executionInfo = mSaveUniqueImpressionsTask.execute();

        verify(mStorage).pushMany(getKeysFromMap(inputMap));
        assertEquals(SplitTaskExecutionStatus.SUCCESS, executionInfo.getStatus());
    }

    private Set<String> getSet(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private List<UniqueKey> getKeysFromMap(Map<String, Set<String>> inputMap) {
        List<UniqueKey> keys = new ArrayList<>();

        for (String key : inputMap.keySet()) {
            keys.add(new UniqueKey(key, inputMap.get(key)));
        }

        return keys;
    }
}
