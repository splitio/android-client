package io.split.android.client.storage.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.sharedtest.helper.TestingHelper;
import io.split.android.client.storage.db.StorageRecordStatus;

public class EventsStorageTest {

    @Mock
    private PersistentEventsStorage mPersistentStorage;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testStartDisabledPersistence() {
        EventsStorage storage = new EventsStorage(mPersistentStorage, false);
        pushEvents(storage);
        verify(mPersistentStorage, times(0)).push(any());
    }

    @Test
    public void testStartEnabledPersistence() {
        EventsStorage storage = new EventsStorage(mPersistentStorage, true);
        pushEvents(storage);
        verify(mPersistentStorage, times(10)).push(any());
    }

    @Test
    public void testEnablePersistence() {

        // When enabling persistence data should be persisted and
        // in memory cache cleared
        EventsStorage storage = new EventsStorage(mPersistentStorage, false);
        pushEvents(storage);
        verify(mPersistentStorage, times(0)).push(any());

        storage.enablePersistence(true);
        pushEvents(storage);
        verify(mPersistentStorage, times(10)).push(any());
    }

    @Test
    public void testDisablePersistence() {

        // When disabling persistence data should not be persisted
        EventsStorage storage = new EventsStorage(mPersistentStorage, true);
        pushEvents(storage);
        verify(mPersistentStorage, times(10)).push(any());

        storage.enablePersistence(false);
        pushEvents(storage);
        verifyNoMoreInteractions(mPersistentStorage);
    }

    @Test
    public void clearInMemory() {
        // When disabling persistence data should not be persisted
        EventsStorage storage = new EventsStorage(mPersistentStorage, false);
        pushEvents(storage);
        storage.clearInMemory();
        storage.enablePersistence(true);

        verify(mPersistentStorage, times(0)).push(any());
    }


    private void pushEvents(EventsStorage storage) {
        TestingHelper.createEvents(0, 9, StorageRecordStatus.ACTIVE)
                .stream().forEach((imp) -> {
                    storage.push(imp);
                });
    }
}
