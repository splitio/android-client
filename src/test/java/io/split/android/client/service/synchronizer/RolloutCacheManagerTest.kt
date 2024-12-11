package io.split.android.client.service.synchronizer

import io.split.android.client.RolloutCacheConfiguration
import io.split.android.client.service.CleanUpDatabaseTask
import io.split.android.client.service.executor.SplitTaskExecutionListener
import io.split.android.client.storage.RolloutDefinitionsCache
import io.split.android.client.storage.cipher.EncryptionMigrationTask
import io.split.android.client.storage.general.GeneralInfoStorage
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.longThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.concurrent.TimeUnit

class RolloutCacheManagerTest {

    private lateinit var mRolloutCacheManager: RolloutCacheManager
    private lateinit var mGeneralInfoStorage: GeneralInfoStorage
    private lateinit var mSplitsCache: RolloutDefinitionsCache
    private lateinit var mSegmentsCache: RolloutDefinitionsCache
    private lateinit var mEncryptionMigrationTask: EncryptionMigrationTask
    private lateinit var mCleanUpDatabaseTask: CleanUpDatabaseTask

    @Before
    fun setup() {
        mGeneralInfoStorage = mock(GeneralInfoStorage::class.java)
        mEncryptionMigrationTask = mock(EncryptionMigrationTask::class.java)
        mCleanUpDatabaseTask = mock(CleanUpDatabaseTask::class.java)
        mSplitsCache = mock(RolloutDefinitionsCache::class.java)
        mSegmentsCache = mock(RolloutDefinitionsCache::class.java)
    }

    @Test
    fun `validateCache calls listener`() {
        mRolloutCacheManager = getCacheManager(10, false)

        val listener = mock(SplitTaskExecutionListener::class.java)
        mRolloutCacheManager.validateCache(listener)

        verify(listener).taskExecuted(any())
    }

    @Test
    fun `validateCache calls clear on storages when expiration is surpassed`() {
        val mockedTimestamp = createMockedTimestamp(10)
        `when`(mGeneralInfoStorage.splitsUpdateTimestamp).thenReturn(mockedTimestamp)
        mRolloutCacheManager = getCacheManager(9, false)

        mRolloutCacheManager.validateCache(mock(SplitTaskExecutionListener::class.java))

        verify(mSplitsCache).clear()
        verify(mSegmentsCache).clear()
    }

    @Test
    fun `validateCache does not call clear on storages when expiration is not surpassed and clearOnInit is false`() {
        val mockedTimestamp = createMockedTimestamp(1L)
        `when`(mGeneralInfoStorage.splitsUpdateTimestamp).thenReturn(mockedTimestamp)
        mRolloutCacheManager = getCacheManager(10, false)

        mRolloutCacheManager.validateCache(mock(SplitTaskExecutionListener::class.java))

        verify(mSplitsCache, times(0)).clear()
        verify(mSegmentsCache, times(0)).clear()
    }

    @Test
    fun `validateCache calls clear on storages when expiration is not surpassed and clearOnInit is true`() {
        val mockedTimestamp = createMockedTimestamp(1L)
        `when`(mGeneralInfoStorage.splitsUpdateTimestamp).thenReturn(mockedTimestamp)
        mRolloutCacheManager = getCacheManager(10, true)

        mRolloutCacheManager.validateCache(mock(SplitTaskExecutionListener::class.java))

        verify(mSplitsCache).clear()
        verify(mSegmentsCache).clear()
    }

    @Test
    fun `validateCache calls clear on storage only once when executed consecutively`() {
        val mockedTimestamp = createMockedTimestamp(1L)
        `when`(mGeneralInfoStorage.splitsUpdateTimestamp).thenReturn(mockedTimestamp)
        `when`(mGeneralInfoStorage.rolloutCacheLastClearTimestamp).thenReturn(0L).thenReturn(TimeUnit.HOURS.toMillis(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis()) - 1))
        mRolloutCacheManager = getCacheManager(10, true)

        mRolloutCacheManager.validateCache(mock(SplitTaskExecutionListener::class.java))
        mRolloutCacheManager.validateCache(mock(SplitTaskExecutionListener::class.java))

        verify(mSplitsCache, times(1)).clear()
        verify(mSegmentsCache, times(1)).clear()
    }

    @Test
    fun `exception during clear still calls listener`() {
        val mockedTimestamp = createMockedTimestamp(1L)
        `when`(mGeneralInfoStorage.splitsUpdateTimestamp).thenReturn(mockedTimestamp)
        `when`(mGeneralInfoStorage.rolloutCacheLastClearTimestamp).thenReturn(0L).thenReturn(TimeUnit.HOURS.toMillis(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis()) - 1))
        mRolloutCacheManager = getCacheManager(10, true)

        val listener = mock(SplitTaskExecutionListener::class.java)
        `when`(mSplitsCache.clear()).thenThrow(RuntimeException("Exception during clear"))

        mRolloutCacheManager.validateCache(listener)

        verify(listener).taskExecuted(any())
    }

    @Test
    fun `validateCache updates last clear timestamp when storages are cleared`() {
        val mockedTimestamp = createMockedTimestamp(1L)
        `when`(mGeneralInfoStorage.splitsUpdateTimestamp).thenReturn(mockedTimestamp)
        `when`(mGeneralInfoStorage.rolloutCacheLastClearTimestamp).thenReturn(0L).thenReturn(TimeUnit.HOURS.toMillis(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis()) - 1))
        mRolloutCacheManager = getCacheManager(10, true)

        mRolloutCacheManager.validateCache(mock(SplitTaskExecutionListener::class.java))

        verify(mGeneralInfoStorage).setRolloutCacheLastClearTimestamp(longThat { it > 0 })
    }

    @Test
    fun `validateCache does not update last clear timestamp when storages are not cleared`() {
        val mockedTimestamp = createMockedTimestamp(1L)
        `when`(mGeneralInfoStorage.splitsUpdateTimestamp).thenReturn(mockedTimestamp)
        `when`(mGeneralInfoStorage.rolloutCacheLastClearTimestamp).thenReturn(0L).thenReturn(TimeUnit.HOURS.toMillis(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis()) - 1))
        mRolloutCacheManager = getCacheManager(10, false)

        mRolloutCacheManager.validateCache(mock(SplitTaskExecutionListener::class.java))

        verify(mGeneralInfoStorage, times(0)).setRolloutCacheLastClearTimestamp(anyLong())
    }

    @Test
    fun `validateCache executes cleanUpDatabaseTask`() {
        mRolloutCacheManager = getCacheManager(10, false)

        mRolloutCacheManager.validateCache(mock(SplitTaskExecutionListener::class.java))

        verify(mCleanUpDatabaseTask).execute()
    }

    @Test
    fun `validateCache executes encryptionMigrationTask`() {
        mRolloutCacheManager = getCacheManager(10, false)

        mRolloutCacheManager.validateCache(mock(SplitTaskExecutionListener::class.java))

        verify(mEncryptionMigrationTask).execute()
    }

    @Test
    fun `default value for update timestamp does not clear cache`() {
        `when`(mGeneralInfoStorage.splitsUpdateTimestamp).thenReturn(0L)
        `when`(mGeneralInfoStorage.rolloutCacheLastClearTimestamp).thenReturn(0L)
        mRolloutCacheManager = getCacheManager(10, false)

        mRolloutCacheManager.validateCache(mock(SplitTaskExecutionListener::class.java))

        verify(mSplitsCache, times(0)).clear()
        verify(mSegmentsCache, times(0)).clear()
    }

    @Test
    fun `default value for last clear timestamp clears cache when clearOnInit is true`() {
        `when`(mGeneralInfoStorage.splitsUpdateTimestamp).thenReturn(createMockedTimestamp(System.currentTimeMillis()))
        `when`(mGeneralInfoStorage.rolloutCacheLastClearTimestamp).thenReturn(0L)
        mRolloutCacheManager = getCacheManager(10, true)

        mRolloutCacheManager.validateCache(mock(SplitTaskExecutionListener::class.java))

        verify(mSplitsCache).clear()
        verify(mSegmentsCache).clear()
    }

    private fun getCacheManager(expiration: Int, clearOnInit: Boolean): RolloutCacheManager {
        return RolloutCacheManagerImpl(mGeneralInfoStorage, RolloutCacheConfiguration.builder().expirationDays(expiration).clearOnInit(clearOnInit).build(), mCleanUpDatabaseTask, mEncryptionMigrationTask, mSplitsCache, mSegmentsCache)
    }

    private fun createMockedTimestamp(period: Long): Long {
        val currentTimeMillis = System.currentTimeMillis()
        val mockedTimestamp =
            TimeUnit.DAYS.toMillis(TimeUnit.MILLISECONDS.toDays(currentTimeMillis) - period)
        return mockedTimestamp
    }
}
