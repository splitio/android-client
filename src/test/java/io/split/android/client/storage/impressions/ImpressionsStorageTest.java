package io.split.android.client.storage.impressions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import helper.TestingHelper;
import io.split.android.client.storage.db.StorageRecordStatus;

public class ImpressionsStorageTest {

    @Mock
    private PersistentImpressionsStorage mPersistentStorage;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testStartDisabledPersistence() {
        ImpressionsStorage storage = new ImpressionsStorage(mPersistentStorage, false);
        pushImpressions(storage);
        verify(mPersistentStorage, times(0)).push(any());
    }

    @Test
    public void testStartEnabledPersistence() {
        ImpressionsStorage storage = new ImpressionsStorage(mPersistentStorage, true);
        pushImpressions(storage);
        verify(mPersistentStorage, times(10)).push(any());
    }

    @Test
    public void testEnablePersistence() {

        // When enabling persistence data should be persisted and
        // in memory cache cleared
        ImpressionsStorage storage = new ImpressionsStorage(mPersistentStorage, false);
        pushImpressions(storage);
        verify(mPersistentStorage, times(0)).push(any());

        storage.enablePersistence(true);
        pushImpressions(storage);
        verify(mPersistentStorage, times(10)).push(any());
    }

    @Test
    public void testDisablePersistence() {

        // When disabling persistence data should not be persisted
        ImpressionsStorage storage = new ImpressionsStorage(mPersistentStorage, true);
        pushImpressions(storage);
        verify(mPersistentStorage, times(10)).push(any());

        storage.enablePersistence(false);
        pushImpressions(storage);
        verify(mPersistentStorage, times(10)).push(any());
    }

    @Test
    public void clearInMemory() {
        // When disabling persistence data should not be persisted
        ImpressionsStorage storage = new ImpressionsStorage(mPersistentStorage, false);
        pushImpressions(storage);
        storage.clearInMemory();
        storage.enablePersistence(true);

        verify(mPersistentStorage, times(0)).push(any());
    }


    private void pushImpressions(ImpressionsStorage storage) {
        TestingHelper.createImpressions(0, 9, StorageRecordStatus.ACTIVE)
                .stream().forEach((imp) -> {
                    storage.push(imp);
                });
    }
}
