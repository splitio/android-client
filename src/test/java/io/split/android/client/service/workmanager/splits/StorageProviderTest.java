package io.split.android.client.service.workmanager.splits;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;

public class StorageProviderTest {

    private SplitRoomDatabase mDatabase;
    private StorageProvider mStorageProvider;

    @Before
    public void setUp() {
        mDatabase = mock(SplitRoomDatabase.class);
        mStorageProvider = new StorageProvider(mDatabase, "sdk-key", true, true);
    }

    @Test
    public void provideSplitsStorageUsesStorageFactory() {
        try (MockedStatic<StorageFactory> mockedStatic = mockStatic(StorageFactory.class)) {
            when(StorageFactory.getSplitsStorageForWorker(mDatabase, "sdk-key", true)).thenReturn(mock(SplitsStorage.class));

            mStorageProvider.provideSplitsStorage();

            mockedStatic.verify(() -> StorageFactory.getSplitsStorageForWorker(mDatabase, "sdk-key", true));
        }
    }

    @Test
    public void provideSplitsStorageWithDisabledEncryptionUsesStorageFactory() {
        mStorageProvider = new StorageProvider(mDatabase, "sdk-key", false, true);
        try (MockedStatic<StorageFactory> mockedStatic = mockStatic(StorageFactory.class)) {
            when(StorageFactory.getSplitsStorageForWorker(mDatabase, "sdk-key", false)).thenReturn(mock(SplitsStorage.class));

            mStorageProvider.provideSplitsStorage();

            mockedStatic.verify(() -> StorageFactory.getSplitsStorageForWorker(mDatabase, "sdk-key", false));
        }
    }

    @Test
    public void provideSplitsStorageCallsLoadLocalOnSplitsStorage() {
        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        try (MockedStatic<StorageFactory> mockedStatic = mockStatic(StorageFactory.class)) {
            when(StorageFactory.getSplitsStorageForWorker(mDatabase, "sdk-key", true)).thenReturn(splitsStorage);

            mStorageProvider.provideSplitsStorage();

            verify(splitsStorage).loadLocal();
        }
    }

    @Test
    public void provideTelemetryStorageUsesStorageFactory() {
        try (MockedStatic<StorageFactory> mockedStatic = mockStatic(StorageFactory.class)) {
            when(StorageFactory.getTelemetryStorage(true)).thenReturn(mock(TelemetryStorage.class));

            mStorageProvider.provideTelemetryStorage();

            mockedStatic.verify(() -> StorageFactory.getTelemetryStorage(true));
        }
    }

    @Test
    public void provideTelemetryStorageWithDisabledTelemetryUsesStorageFactory() {
        mStorageProvider = new StorageProvider(mDatabase, "sdk-key", true, false);
        try (MockedStatic<StorageFactory> mockedStatic = mockStatic(StorageFactory.class)) {
            when(StorageFactory.getTelemetryStorage(false)).thenReturn(mock(TelemetryStorage.class));

            StorageProvider storageProvider = new StorageProvider(mDatabase, "sdk-key", true, false);
            storageProvider.provideTelemetryStorage();

            mockedStatic.verify(() -> StorageFactory.getTelemetryStorage(false));
        }
    }
}
