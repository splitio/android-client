package io.split.android.client.service.workmanager.splits;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorageProducer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;

public class StorageProviderTest {

    private SplitRoomDatabase mDatabase;
    private StorageProvider mStorageProvider;

    @Before
    public void setUp() {
        mDatabase = mock(SplitRoomDatabase.class);
        mStorageProvider = getStorageProvider("sdk-key", true, true);
    }

    @NonNull
    private StorageProvider getStorageProvider(String apiKey, boolean encryptionEnabled, boolean shouldRecordTelemetry) {
        return new StorageProvider(mDatabase, apiKey, encryptionEnabled, shouldRecordTelemetry);
    }

    @Test
    public void provideSplitsStorageUsesStorageFactory() {
        try (MockedStatic<StorageFactory> mockedStatic = mockStatic(StorageFactory.class)) {
            try (MockedStatic<SplitCipherFactory> mockedStaticCipher = mockStatic(SplitCipherFactory.class)) {
                SplitCipher cipher = mock(SplitCipher.class);
                SplitsStorage splitStorage = mock(SplitsStorage.class);
                when(StorageFactory.getSplitsStorage(mDatabase, cipher)).thenReturn(splitStorage);
                when(SplitCipherFactory.create("sdk-key", true)).thenReturn(cipher);
                mStorageProvider = getStorageProvider("sdk-key", true, true);

                mStorageProvider.provideSplitsStorage();

                mockedStaticCipher.verify(() -> SplitCipherFactory.create("sdk-key", true));
                mockedStatic.verify(() -> StorageFactory.getSplitsStorage(mDatabase, cipher));
            }
        }
    }

    @Test
    public void provideSplitsStorageWithDisabledEncryptionUsesStorageFactory() {
        try (MockedStatic<StorageFactory> mockedStatic = mockStatic(StorageFactory.class)) {
            try (MockedStatic<SplitCipherFactory> mockedStaticCipher = mockStatic(SplitCipherFactory.class)) {
                SplitCipher cipher = mock(SplitCipher.class);
                SplitsStorage splitStorage = mock(SplitsStorage.class);
                when(StorageFactory.getSplitsStorage(mDatabase, cipher)).thenReturn(splitStorage);
                when(SplitCipherFactory.create("sdk-key", false)).thenReturn(cipher);
                mStorageProvider = getStorageProvider("sdk-key", false, true);

                mStorageProvider.provideSplitsStorage();

                mockedStaticCipher.verify(() -> SplitCipherFactory.create("sdk-key", false));
                mockedStatic.verify(() -> StorageFactory.getSplitsStorage(mDatabase, cipher));
            }
        }
    }

    @Test
    public void provideSplitsStorageCallsLoadLocalOnSplitsStorage() {
        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        try (MockedStatic<StorageFactory> mockedStatic = mockStatic(StorageFactory.class)) {
            try (MockedStatic<SplitCipherFactory> mockedStaticCipher = mockStatic(SplitCipherFactory.class)) {
                SplitCipher cipher = mock(SplitCipher.class);
                when(SplitCipherFactory.create("sdk-key", true)).thenReturn(cipher);
                when(StorageFactory.getSplitsStorage(mDatabase, cipher)).thenReturn(splitsStorage);
                mStorageProvider = getStorageProvider("sdk-key", true, true);

                mStorageProvider.provideSplitsStorage();

                verify(splitsStorage).loadLocal();
            }
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

    @Test
    public void provideRuleBasedSegmentStorageProducerUsesStorageFactory() {
        try (MockedStatic<StorageFactory> mockedStatic = mockStatic(StorageFactory.class)) {
            try (MockedStatic<SplitCipherFactory> mockedStaticCipher = mockStatic(SplitCipherFactory.class)) {
                SplitCipher cipher = mock(SplitCipher.class);
                when(SplitCipherFactory.create("sdk-key", true)).thenReturn(cipher);
                when(StorageFactory.getRuleBasedSegmentStorageForWorker(mDatabase, cipher)).thenReturn(mock(RuleBasedSegmentStorageProducer.class));
                mStorageProvider = getStorageProvider("sdk-key", true, true);

                mStorageProvider.provideRuleBasedSegmentStorage();

                mockedStaticCipher.verify(() -> SplitCipherFactory.create("sdk-key", true));
                mockedStatic.verify(() -> StorageFactory.getRuleBasedSegmentStorageForWorker(mDatabase, cipher));
            }
        }
    }

    @Test
    public void provideRuleBasedSegmentStoargeProducerCallsLoadLocalOnRuleBasedSegmentStorageProducer() {
        RuleBasedSegmentStorageProducer ruleBasedSegmentStorageProducer = mock(RuleBasedSegmentStorageProducer.class);
        try (MockedStatic<StorageFactory> mockedStatic = mockStatic(StorageFactory.class)) {
            try (MockedStatic<SplitCipherFactory> mockedStaticCipher = mockStatic(SplitCipherFactory.class)) {
                SplitCipher cipher = mock(SplitCipher.class);
                when(SplitCipherFactory.create("sdk-key", true)).thenReturn(cipher);
                when(StorageFactory.getRuleBasedSegmentStorageForWorker(mDatabase, cipher)).thenReturn(ruleBasedSegmentStorageProducer);
                mStorageProvider = getStorageProvider("sdk-key", true, true);

                mStorageProvider.provideRuleBasedSegmentStorage();

                verify(ruleBasedSegmentStorageProducer).loadLocal();
            }
        }
    }

    @Test
    public void provideRuleBasedSegmentStorageProducerWithDisabledEncryptionUsesStorageFactory() {
        try (MockedStatic<StorageFactory> mockedStatic = mockStatic(StorageFactory.class)) {
            try (MockedStatic<SplitCipherFactory> mockedStaticCipher = mockStatic(SplitCipherFactory.class)) {
                SplitCipher cipher = mock(SplitCipher.class);
                when(SplitCipherFactory.create("sdk-key", false)).thenReturn(cipher);
                when(StorageFactory.getRuleBasedSegmentStorageForWorker(mDatabase, cipher)).thenReturn(mock(RuleBasedSegmentStorageProducer.class));
                mStorageProvider = getStorageProvider("sdk-key", false, true);

                mStorageProvider.provideRuleBasedSegmentStorage();

                mockedStaticCipher.verify(() -> SplitCipherFactory.create("sdk-key", false));
                mockedStatic.verify(() -> StorageFactory.getRuleBasedSegmentStorageForWorker(mDatabase, cipher));
            }
        }
    }
}
