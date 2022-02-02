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
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutorFactory;
import io.split.android.client.storage.db.SplitEntity;

public class SplitEntityConverterImplTest {

    @Mock
    private SplitParallelTaskExecutor<List<Split>> mSplitTaskExecutor;
    @Mock
    private SplitParallelTaskExecutor<List<SplitEntity>> mEntityTaskExecutor;
    @Mock
    private SplitParallelTaskExecutorFactory mTaskExecutorFactory;
    private SplitEntityConverterImpl mConverter;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mSplitTaskExecutor.getAvailableThreads()).thenReturn(2);
        when(mEntityTaskExecutor.getAvailableThreads()).thenReturn(2);

        when(mTaskExecutorFactory.createForList(Split.class)).thenReturn(mSplitTaskExecutor);
        when(mTaskExecutorFactory.createForList(SplitEntity.class)).thenReturn(mEntityTaskExecutor);

        mConverter = new SplitEntityConverterImpl(mTaskExecutorFactory);
    }

    @Test
    public void tasksAreCreatedAccordingToTheAmountOfThreadsAvailable() {
        ArgumentCaptor<List<SplitDeferredTaskItem<List<Split>>>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        when(mSplitTaskExecutor.getAvailableThreads()).thenReturn(4);
        List<SplitEntity> mockEntities = getMockEntities(65);

        int expectedNumberOfLists = 5;

        mConverter.getFromEntityList(mockEntities);

        verify(mSplitTaskExecutor).execute(argumentCaptor.capture());
        assertEquals(expectedNumberOfLists, argumentCaptor.getValue().size());
    }

    @Test
    public void amountOfSplitsEqualsAmountOfEntities() {
        when(mSplitTaskExecutor.getAvailableThreads()).thenReturn(4);
        List<SplitEntity> mockEntities = getMockEntities(3);

        List<Split> splits = mConverter.getFromEntityList(mockEntities);

        assertEquals(3, splits.size());
    }

    @Test
    public void amountOfSplitsEqualsAmountOfEntitiesWhenParallel() {
        when(mSplitTaskExecutor.getAvailableThreads()).thenReturn(2);
        List<SplitEntity> mockEntities = getMockEntities(3);
        when(mSplitTaskExecutor.execute(any())).thenReturn(
                Arrays.asList(Collections.singletonList(new Split()),
                        Collections.singletonList(new Split()),
                        Collections.singletonList(new Split())));

        List<Split> splits = mConverter.getFromEntityList(mockEntities);

        assertEquals(3, splits.size());
    }

    private List<SplitEntity> getMockEntities(int amount) {
        ArrayList<SplitEntity> entities = new ArrayList<>();
        String jsonTemplate = "{\"name\":\"%s\", \"changeNumber\": %d}";
        long initialChangeNumber = 9999;

        for (int i = 0; i < amount; i++) {
            String splitName = "split-" + i;
            SplitEntity entity = new SplitEntity();
            entity.setName(splitName);
            entity.setBody(String.format(jsonTemplate, splitName, initialChangeNumber - i));
            entities.add(entity);
        }

        return entities;
    }
}
