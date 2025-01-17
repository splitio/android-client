package io.split.android.client

import android.content.Context
import io.split.android.client.SplitFactoryHelper.Initializer.Listener
import io.split.android.client.events.EventsManagerCoordinator
import io.split.android.client.events.SplitInternalEvent
import io.split.android.client.lifecycle.SplitLifecycleManager
import io.split.android.client.service.executor.SplitSingleThreadTaskExecutor
import io.split.android.client.service.executor.SplitTaskExecutionInfo
import io.split.android.client.service.executor.SplitTaskExecutionListener
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.service.executor.SplitTaskType
import io.split.android.client.service.synchronizer.RolloutCacheManager
import io.split.android.client.service.synchronizer.SyncManager
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.io.File
import java.lang.IllegalArgumentException

class SplitFactoryHelperTest {

    private lateinit var mocks: AutoCloseable

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
    fun generateDatabaseNameWithoutPrefixAndKeyLongerThan4() {
        val path = mock(File::class.java)
        `when`(path.exists()).thenReturn(true)
        `when`(context.getDatabasePath("abcdwxyz")).thenReturn(path)
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
        `when`(context.getDatabasePath("split_data")).thenReturn(path)
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
        `when`(context.getDatabasePath("mydbabcdwxyz")).thenReturn(path)
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
        `when`(context.getDatabasePath("mydbsplit_data")).thenReturn(path)
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

    @Test
    fun legacyDbIsRenamedIfExists() {
        val nonExistingPath = mock(File::class.java)
        val existingPath = mock(File::class.java)
        `when`(nonExistingPath.exists()).thenReturn(false)
        `when`(existingPath.exists()).thenReturn(true)
        `when`(context.getDatabasePath(any())).thenReturn(existingPath);
        `when`(context.getDatabasePath("abcdwxyz")).thenReturn(nonExistingPath)
        val databaseName = helper.getDatabaseName(
            SplitClientConfig.builder().build(),
            "abcdfghijklmnopqrstuvwxyz",
            context
        )

        verify(existingPath).renameTo(nonExistingPath)
        assertEquals("abcdwxyz", databaseName)
    }

    @Test
    fun `Initializer test`() {
        val rolloutCacheManager = mock(RolloutCacheManager::class.java)
        val splitTaskExecutionListener = mock(SplitTaskExecutionListener::class.java)

        val initializer = SplitFactoryHelper.Initializer(
            rolloutCacheManager,
            splitTaskExecutionListener
        )

        initializer.run()

        verify(rolloutCacheManager).validateCache(splitTaskExecutionListener)
    }

    @Test
    fun `Initializer Listener test`() {
        val eventsManagerCoordinator = mock(EventsManagerCoordinator::class.java)
        val taskExecutor = mock(SplitTaskExecutor::class.java)
        val singleThreadTaskExecutor = mock(SplitSingleThreadTaskExecutor::class.java)
        val syncManager = mock(SyncManager::class.java)
        val lifecycleManager = mock(SplitLifecycleManager::class.java)

        val listener = Listener(
            eventsManagerCoordinator,
            taskExecutor,
            singleThreadTaskExecutor,
            syncManager,
            lifecycleManager
        )

        listener.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK))

        verify(eventsManagerCoordinator).notifyInternalEvent(SplitInternalEvent.ENCRYPTION_MIGRATION_DONE)
        verify(taskExecutor).resume()
        verify(singleThreadTaskExecutor).resume()
        verify(syncManager).start()
        verify(lifecycleManager).register(syncManager)
    }
}
