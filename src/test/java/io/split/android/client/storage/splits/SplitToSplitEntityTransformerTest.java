package io.split.android.client.storage.splits;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.parallel.SplitDeferredTaskItem;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutor;
import io.split.android.client.storage.db.SplitEntity;

public class SplitToSplitEntityTransformerTest {

    @Mock
    private SplitParallelTaskExecutor<List<SplitEntity>> mSplitTaskExecutor;
    private SplitToSplitEntityTransformer mConverter;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mConverter = new SplitToSplitEntityTransformer(mSplitTaskExecutor);
    }

    @Test
    public void tasksAreCreatedAccordingToTheAmountOfThreadsAvailable() {
        ArgumentCaptor<List<SplitDeferredTaskItem<List<SplitEntity>>>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        when(mSplitTaskExecutor.getAvailableThreads()).thenReturn(4);
        List<Split> mockEntities = getMockSplits(65);

        int expectedNumberOfLists = 5;

        mConverter.transform(mockEntities);

        verify(mSplitTaskExecutor).execute(argumentCaptor.capture());
        assertEquals(expectedNumberOfLists, argumentCaptor.getValue().size());
    }

    @Test
    public void amountOfSplitsEqualsAmountOfEntities() {
        when(mSplitTaskExecutor.getAvailableThreads()).thenReturn(4);
        List<Split> mockEntities = getMockSplits(3);

        List<SplitEntity> splits = mConverter.transform(mockEntities);

        assertEquals(3, splits.size());
    }

    @Test
    public void amountOfSplitsEqualsAmountOfEntitiesWhenParallel() {
        when(mSplitTaskExecutor.getAvailableThreads()).thenReturn(2);
        List<Split> mockEntities = getMockSplits(3);
        when(mSplitTaskExecutor.execute(any())).thenReturn(
                Arrays.asList(Collections.singletonList(new SplitEntity()),
                        Collections.singletonList(new SplitEntity()),
                        Collections.singletonList(new SplitEntity())));

        List<SplitEntity> splits = mConverter.transform(mockEntities);

        assertEquals(3, splits.size());
    }

    private List<Split> getMockSplits(int size) {
        List<Split> splits = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            splits.add(new Split());
        }

        return splits;
    }
}
