package io.split.android.client

import android.content.Context
import io.split.android.client.service.executor.SplitTaskExecutionListener
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.storage.cipher.EncryptionMigrationTask
import io.split.android.client.storage.db.SplitRoomDatabase
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.argThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.io.File
import java.lang.IllegalArgumentException

class SplitFactoryHelperTest {

    private lateinit var mocks: AutoCloseable

    @Mock
    private lateinit var splitRoomDatabase: SplitRoomDatabase
    @Mock
    private lateinit var splitTaskExecutor: SplitTaskExecutor
    @Mock
    private lateinit var taskListener: SplitTaskExecutionListener
    @Mock
    private lateinit var context: Context

    private lateinit var helper: SplitFactoryHelper

    @Before
    fun setup() {
        mocks = MockitoAnnotations.openMocks(this)
        helper = SplitFactoryHelper()
    }

    @After
    fun tearDown() {
        mocks.close()
    }

    @Test
    fun migrateEncryption() {

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

    @Test
    fun generateDatabaseNameWithoutPrefixAndKeyLongerThan4() {
        val path = mock(File::class.java)
        `when`(path.exists()).thenReturn(true)
        `when`(context.getDatabasePath(Mockito.anyString())).thenReturn(path)
        val databaseName = helper.getDatabaseName(
            SplitClientConfig.builder().build(),
            "abcdedfghijklmnopqrstuvwxyz",
            context
        )

        assertEquals("abcdwxyz", databaseName)
    }

    @Test
    fun generateDatabaseNameWithoutPrefixAndKeyShorterThan4() {
        val path = mock(File::class.java)
        `when`(path.exists()).thenReturn(true)
        `when`(context.getDatabasePath(Mockito.anyString())).thenReturn(path)
        val databaseName = helper.getDatabaseName(
            SplitClientConfig.builder().build(),
            "abc",
            context
        )

        assertEquals("split_data", databaseName)
    }

    @Test
    fun generateDatabaseNameWithPrefixAndKeyLongerThan4() {
        val path = mock(File::class.java)
        `when`(path.exists()).thenReturn(true)
        `when`(context.getDatabasePath(Mockito.anyString())).thenReturn(path)
        val databaseName = helper.getDatabaseName(
            SplitClientConfig.builder().prefix("mydb").build(),
            "abcdedfghijklmnopqrstuvwxyz",
            context
        )

        assertEquals("mydbabcdwxyz", databaseName)
    }

    @Test
    fun generateDatabaseNameWithPrefixAndKeyShorterThan4() {
        val path = mock(File::class.java)
        `when`(path.exists()).thenReturn(true)
        `when`(context.getDatabasePath(Mockito.anyString())).thenReturn(path)
        val databaseName = helper.getDatabaseName(
            SplitClientConfig.builder().prefix("mydb").build(),
            "abc",
            context
        )

        assertEquals("mydbsplit_data", databaseName)
    }

    @Test
    fun generateDatabaseNameWithNullKeyThrowsIllegalArgumentException() {
        val path = mock(File::class.java)
        `when`(path.exists()).thenReturn(true)
        `when`(context.getDatabasePath(Mockito.anyString())).thenReturn(path)

        try {
            helper.getDatabaseName(
                SplitClientConfig.builder().build(),
                null,
                context
            )
        } catch (e: IllegalArgumentException) {
            assertEquals("SDK key cannot be null", e.message)
        }
    }
}
