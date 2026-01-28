package io.split.android.client.storage.rbs;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.general.GeneralInfoStorage;

public class SqLiteRuleBasedSegmentsPersistentStorageProviderTest {

    @Test
    public void providesSqLiteImplementation() {
        PersistentRuleBasedSegmentStorage.Provider provider =
                new SqLitePersistentRuleBasedSegmentStorageProvider(mock(SplitCipher.class), mock(SplitRoomDatabase.class), mock(GeneralInfoStorage.class));
        PersistentRuleBasedSegmentStorage persistentRuleBasedSegmentStorage = provider.get();

        assertTrue(persistentRuleBasedSegmentStorage instanceof SqLitePersistentRuleBasedSegmentStorage);
    }
}
