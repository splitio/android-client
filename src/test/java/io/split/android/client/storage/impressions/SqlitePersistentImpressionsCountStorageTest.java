package io.split.android.client.storage.impressions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonParseException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import io.split.android.client.service.impressions.ImpressionsCountPerFeature;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.ImpressionsCountDao;
import io.split.android.client.storage.db.ImpressionsCountEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;

public class SqlitePersistentImpressionsCountStorageTest {

    @Mock
    private SplitRoomDatabase mDatabase;

    @Mock
    private ImpressionsCountDao mDao;

    @Mock
    private SplitCipher mSplitCipher;

    private SqLitePersistentImpressionsCountStorage mStorage;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mDatabase.impressionsCountDao()).thenReturn(mDao);
        mStorage = new SqLitePersistentImpressionsCountStorage(mDatabase, 1000L, mSplitCipher);
    }

    @Test
    public void entityIsInsertedUsingDao() {
        ImpressionsCountEntity entity = mStorage.entityForModel(createTestImpressionsCountPerFeature());

        mStorage.insert(entity);

        verify(mDao).insert(entity);
    }

    @Test
    public void entitiesAreInsertedUsingDao() {
        List<ImpressionsCountEntity> entities = Arrays.asList(mStorage.entityForModel(createTestImpressionsCountPerFeature()),
                mStorage.entityForModel(createTestImpressionsCountPerFeature()));

        mStorage.insert(entities);

        verify(mDao).insert(entities);
    }

    @Test
    public void entityForModelEncryptsBody() {
        ImpressionsCountPerFeature count = createTestImpressionsCountPerFeature();
        when(mSplitCipher.encrypt(convertToJson())).thenReturn("encrypted_body");

        mStorage.entityForModel(count);

        verify(mSplitCipher).encrypt(convertToJson());
    }

    @Test
    public void deleteByStatusUsesDao() {
        int status = 1;
        long maxTimestamp = 1234567890L;

        mStorage.deleteByStatus(status, maxTimestamp);

        verify(mDao).deleteByStatus(status, maxTimestamp, 100);
    }

    @Test
    public void deleteOutdatedUsesDao() {
        mStorage.deleteOutdated(1234567890L);

        verify(mDao).deleteOutdated(1234567890L);
    }

    @Test
    public void deleteByIdUsesDao() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        mStorage.deleteById(ids);

        verify(mDao).delete(ids);
    }

    @Test
    public void updateStatusUsesDao() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        mStorage.updateStatus(ids, 1);

        verify(mDao).updateStatus(ids, 1);
    }

    @Test
    public void runInTransaction() {
        List<ImpressionsCountEntity> entities = Arrays.asList(mStorage.entityForModel(createTestImpressionsCountPerFeature()),
                mStorage.entityForModel(createTestImpressionsCountPerFeature()));

        mStorage.runInTransaction(entities, 2, 1000L);

        verify(mDatabase).runInTransaction(any(SqLitePersistentImpressionsCountStorage.GetAndUpdate.class));
    }

    @Test
    public void entityForModelDecryptsEntityBody() throws JsonParseException {
        ImpressionsCountPerFeature count = createTestImpressionsCountPerFeature();
        ImpressionsCountEntity entity = mStorage.entityForModel(count);
        when(mSplitCipher.decrypt(entity.getBody())).thenReturn(convertToJson());

        mStorage.entityToModel(entity);

        verify(mSplitCipher).decrypt(entity.getBody());
    }

    private ImpressionsCountPerFeature createTestImpressionsCountPerFeature() {
        return new ImpressionsCountPerFeature("feature", 100, 5);
    }

    private String convertToJson() {
        return "{\"f\":\"feature\",\"m\":100,\"rc\":5}";
    }
}
