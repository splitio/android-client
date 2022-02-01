package io.split.android.client.storage.splits;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.storage.db.SplitDao;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;

public class SqLitePersistentSplitsStorageTest {

    @Mock
    private SplitRoomDatabase mDatabase;
    @Mock
    private SplitEntityConverter mSplitEntityConverter;
    @Mock
    private SplitDao mSplitDao;
    private SqLitePersistentSplitsStorage mStorage;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mStorage = new SqLitePersistentSplitsStorage(mDatabase, mSplitEntityConverter);
    }

    @Test
    public void getAllUsesConverter() {
        List<SplitEntity> mockEntities = getMockEntities();
        when(mSplitDao.getAll()).thenReturn(mockEntities);
        when(mDatabase.splitDao()).thenReturn(mSplitDao);

        mStorage.getAll();

        verify(mSplitEntityConverter).getFromEntityList(mockEntities);
    }

    private List<SplitEntity> getMockEntities() {
        ArrayList<SplitEntity> entities = new ArrayList<>();
        String jsonTemplate = "{\"name\":\"%s\", \"changeNumber\": %d}";
        long initialChangeNumber = 9999;

        for (int i = 0; i < 3; i++) {
            String splitName = "split-" + i;
            SplitEntity entity = new SplitEntity();
            entity.setName(splitName);
            entity.setBody(String.format(jsonTemplate, splitName, initialChangeNumber - i));
            entities.add(entity);
        }

        return entities;
    }
}
