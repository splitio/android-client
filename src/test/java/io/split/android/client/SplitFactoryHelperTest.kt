package io.split.android.client

import android.content.Context
import io.split.android.client.SplitFactoryHelper.Initializer.Listener
import io.split.android.client.api.Key
import io.split.android.client.events.EventsManagerCoordinator
import io.split.android.client.events.SplitInternalEvent
import io.split.android.client.exceptions.SplitInstantiationException
import io.split.android.client.lifecycle.SplitLifecycleManager
import io.split.android.client.network.HttpProxy
import io.split.android.client.network.ProxyConfiguration
import io.split.android.client.service.executor.SplitSingleThreadTaskExecutor
import io.split.android.client.service.executor.SplitTaskExecutionInfo
import io.split.android.client.service.executor.SplitTaskExecutionListener
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.service.executor.SplitTaskType
import io.split.android.client.service.synchronizer.RolloutCacheManager
import io.split.android.client.service.synchronizer.SyncManager
import io.split.android.client.service.synchronizer.WorkManagerWrapper
import io.split.android.client.storage.general.GeneralInfoStorage
import io.split.android.client.utils.HttpProxySerializer
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.io.File
import java.util.concurrent.locks.ReentrantLock

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
        val initLock = mock(ReentrantLock::class.java)

        val initializer = SplitFactoryHelper.Initializer(
            rolloutCacheManager,
            splitTaskExecutionListener,
            initLock
        )

        initializer.run()

        verify(rolloutCacheManager).validateCache(splitTaskExecutionListener)
        verify(initLock).lock()
    }

    @Test
    fun `Initializer Listener test`() {
        val eventsManagerCoordinator = mock(EventsManagerCoordinator::class.java)
        val taskExecutor = mock(SplitTaskExecutor::class.java)
        val singleThreadTaskExecutor = mock(SplitSingleThreadTaskExecutor::class.java)
        val syncManager = mock(SyncManager::class.java)
        val lifecycleManager = mock(SplitLifecycleManager::class.java)
        val initLock = mock(ReentrantLock::class.java)

        val listener = Listener(
            eventsManagerCoordinator,
            taskExecutor,
            singleThreadTaskExecutor,
            syncManager,
            lifecycleManager,
            initLock
        )

        listener.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK))

        verify(eventsManagerCoordinator).notifyInternalEvent(SplitInternalEvent.ENCRYPTION_MIGRATION_DONE)
        verify(taskExecutor).resume()
        verify(singleThreadTaskExecutor).resume()
        verify(syncManager).start()
        verify(lifecycleManager).register(syncManager)
        verify(initLock).unlock()
    }

    @Test
    fun `initializing with proxy config with null url throws`() {
        var exceptionThrown = false
        try {
            SplitFactoryBuilder.build("sdk_key", Key("user"), SplitClientConfig.builder().proxyConfiguration(
                ProxyConfiguration.builder().build()).build(), context)
        } catch (splitInstantiationException: SplitInstantiationException) {
            exceptionThrown = (splitInstantiationException.message ?: "").contains("When configured, proxy host cannot be null")
        }

        assertTrue(exceptionThrown)
    }

    @Test
    fun `initializing with proxy config with valid url does not throw`() {
        var exceptionThrown = false
        try {
            SplitFactoryBuilder.build("sdk_key", Key("user"), SplitClientConfig.builder().proxyConfiguration(
                ProxyConfiguration.builder().url("http://localhost:8080").build()).build(), context)
        } catch (splitInstantiationException: SplitInstantiationException) {
            exceptionThrown = (splitInstantiationException.message ?: "").contains("When configured, proxy host cannot be null")
        }

        assertFalse(exceptionThrown)
    }

    @Test
    fun `setupProxyForBackgroundSync should start thread when proxy is not null, not legacy, and background sync is enabled`() {
        val httpProxy = mock(HttpProxy::class.java)
        val config = mock(SplitClientConfig::class.java)
        val proxyConfigSaveTask = mock(Runnable::class.java)
        
        `when`(config.proxy()).thenReturn(httpProxy)
        `when`(httpProxy.isLegacy()).thenReturn(false)
        `when`(config.synchronizeInBackground()).thenReturn(true)
        
        SplitFactoryHelper.setupProxyForBackgroundSync(config, proxyConfigSaveTask)
        
        Thread.sleep(100) // Give the thread time to start
    }
    
    @Test
    fun `setupProxyForBackgroundSync should not start thread when proxy is null`() {
        val config = mock(SplitClientConfig::class.java)
        val proxyConfigSaveTask = mock(Runnable::class.java)
        
        `when`(config.proxy()).thenReturn(null)
        `when`(config.synchronizeInBackground()).thenReturn(true)
        
        SplitFactoryHelper.setupProxyForBackgroundSync(config, proxyConfigSaveTask)
        
        Thread.sleep(100) // Give time to ensure no thread was started
    }
    
    @Test
    fun `setupProxyForBackgroundSync should not start thread when proxy is legacy`() {
        val httpProxy = mock(HttpProxy::class.java)
        val config = mock(SplitClientConfig::class.java)
        val proxyConfigSaveTask = mock(Runnable::class.java)
        
        `when`(config.proxy()).thenReturn(httpProxy)
        `when`(httpProxy.isLegacy()).thenReturn(true)
        `when`(config.synchronizeInBackground()).thenReturn(true)
        
        SplitFactoryHelper.setupProxyForBackgroundSync(config, proxyConfigSaveTask)
        
        Thread.sleep(100) // Give time to ensure no thread was started
    }
    
    @Test
    fun `setupProxyForBackgroundSync should not start thread when background sync is disabled`() {
        val httpProxy = mock(HttpProxy::class.java)
        val config = mock(SplitClientConfig::class.java)
        val proxyConfigSaveTask = mock(Runnable::class.java)
        
        `when`(config.proxy()).thenReturn(httpProxy)
        `when`(httpProxy.isLegacy()).thenReturn(false)
        `when`(config.synchronizeInBackground()).thenReturn(false)
        
        SplitFactoryHelper.setupProxyForBackgroundSync(config, proxyConfigSaveTask)
        
        Thread.sleep(100) // Give time to ensure no thread was started
    }
    
    @Test
    fun `getProxyConfigSaveTask should return runnable that saves proxy config`() {
        val config = mock(SplitClientConfig::class.java)
        val httpProxy = mock(HttpProxy::class.java)
        val workManagerWrapper = mock(WorkManagerWrapper::class.java)
        val generalInfoStorage = mock(GeneralInfoStorage::class.java)
        val serializedProxy = "serialized_proxy_json"
        
        `when`(config.proxy()).thenReturn(httpProxy)
        
        mockStatic(HttpProxySerializer::class.java).use { mockedSerializer ->
            mockedSerializer.`when`<String> { HttpProxySerializer.serialize(httpProxy) }.thenReturn(serializedProxy)
            
            val runnable = SplitFactoryHelper.getProxyConfigSaveTask(config, workManagerWrapper, generalInfoStorage)
            runnable.run()
            
            verify(generalInfoStorage).setProxyConfig(serializedProxy)
            verify(workManagerWrapper, never()).removeWork()
        }
    }
    
    @Test
    fun `getProxyConfigSaveTask should handle exceptions and disable background sync`() {
        val config = mock(SplitClientConfig::class.java)
        val httpProxy = mock(HttpProxy::class.java)
        val workManagerWrapper = mock(WorkManagerWrapper::class.java)
        val generalInfoStorage = mock(GeneralInfoStorage::class.java)
        
        `when`(config.proxy()).thenReturn(httpProxy)
        
        mockStatic(HttpProxySerializer::class.java).use { mockedSerializer ->
            mockedSerializer.`when`<String> { HttpProxySerializer.serialize(httpProxy) }.thenThrow(RuntimeException("Test exception"))
            
            val runnable = SplitFactoryHelper.getProxyConfigSaveTask(config, workManagerWrapper, generalInfoStorage)
            runnable.run()
            
            verify(generalInfoStorage, never()).setProxyConfig(any())
            verify(workManagerWrapper).removeWork()
        }
    }
}
