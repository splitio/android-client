package io.split.android.client.storage.splits;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.parallel.SplitDeferredTaskItem;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutor;
import io.split.android.client.storage.db.SplitDao;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;

public class SqLitePersistentSplitsStorageTest {

    @Mock
    private SplitRoomDatabase mDatabase;
    @Mock
    private SplitParallelTaskExecutor<List<Split>> mParallelTaskExecutor;
    @Mock
    private SplitDao mSplitDao;
    private SqLitePersistentSplitsStorage mStorage;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mStorage = new SqLitePersistentSplitsStorage(mDatabase, mParallelTaskExecutor);
    }

    @Test
    public void getAllUsesParallelExecutorWhenThereAreMoreThan50Splits() {
        when(mSplitDao.getAll()).thenReturn(getMockEntities(51));
        when(mDatabase.splitDao()).thenReturn(mSplitDao);

        mStorage.getAll();

        verify(mParallelTaskExecutor).execute(anyCollection());
    }

    @Test
    public void tasksAreCreatedAccordingToTheAmountOfThreadsAvailable() {
        ArgumentCaptor<List<SplitDeferredTaskItem<List<Split>>>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        when(mParallelTaskExecutor.getAvailableThreads()).thenReturn(4);
        List<SplitEntity> mockEntities = getMockEntities(65);
        when(mSplitDao.getAll()).thenReturn(mockEntities);
        when(mDatabase.splitDao()).thenReturn(mSplitDao);

        int expectedNumberOfLists = 5;

        mStorage.getAll();

        verify(mParallelTaskExecutor).execute(argumentCaptor.capture());
        assertEquals(expectedNumberOfLists, argumentCaptor.getValue().size());
    }

    private List<SplitEntity> getMockEntities(int amount) {
        ArrayList<SplitEntity> entities = new ArrayList();
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
