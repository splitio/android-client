package io.split.android.client

import io.split.android.client.service.executor.SplitTaskExecutionListener
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.storage.cipher.EncryptionMigrationTask
import io.split.android.client.storage.db.SplitRoomDatabase
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.argThat
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class SplitFactoryHelperTest {

    @Mock
    private lateinit var splitRoomDatabase: SplitRoomDatabase
    @Mock
    private lateinit var splitTaskExecutor: SplitTaskExecutor
    @Mock
    private lateinit var taskListener: SplitTaskExecutionListener

    private lateinit var helper: SplitFactoryHelper

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        helper = SplitFactoryHelper()
    }

    @Test
    fun testMigrateEncryption() {

        helper.migrateEncryption(
            "abcdedfghijklmnopqrstuvwxyz",
            splitRoomDatabase,
            splitTaskExecutor,
            true,
            taskListener,
        )

        verify(splitTaskExecutor).submit(
            argThat { it is EncryptionMigrationTask },
            argThat { it?.equals(taskListener) == true })
    }
}
