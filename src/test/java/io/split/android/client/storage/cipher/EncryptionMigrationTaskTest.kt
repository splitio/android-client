package io.split.android.client.storage.cipher

import io.split.android.client.service.executor.SplitTaskExecutionStatus
import io.split.android.client.service.executor.SplitTaskType
import io.split.android.client.storage.db.GeneralInfoDao
import io.split.android.client.storage.db.GeneralInfoEntity
import io.split.android.client.storage.db.SplitRoomDatabase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.argThat
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference


class EncryptionMigrationTaskTest {

    private val mApiKey: String = "abcdedfghijklmnopqrstuvwxyz"
    @Mock
    private lateinit var mSplitDatabase: SplitRoomDatabase
    @Mock
    private lateinit var mGeneralInfoDao: GeneralInfoDao
    @Mock
    private lateinit var mSplitCipherReference: AtomicReference<SplitCipher>
    @Mock
    private lateinit var mFromCipher: SplitCipher
    @Mock
    private lateinit var mToCipher: SplitCipher
    private lateinit var mCountDownLatch: CountDownLatch

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mCountDownLatch = CountDownLatch(1)
        `when`(mSplitDatabase.generalInfoDao()).thenReturn(mGeneralInfoDao)
    }

    @Test
    fun fromEncryptionLevelIsRetrievedFromGeneralInfo() {
        val encryptionMigrationTask = EncryptionMigrationTask(
            mApiKey,
            mSplitDatabase,
            mSplitCipherReference,
            mCountDownLatch,
            false
        )
        `when`(mGeneralInfoDao.getByName("databaseEncryptionMode")).thenReturn(
            GeneralInfoEntity(
                "databaseEncryptionMode",
                "NONE"
            )
        )

        encryptionMigrationTask.execute()

        verify(mGeneralInfoDao).getByName("databaseEncryptionMode")
    }

    @Test
    fun targetEncryptionLevelIsDeterminedWithEncryptionEnabledProperty() {
        val encryptionMigrationTask = EncryptionMigrationTask(
            mApiKey,
            mSplitDatabase,
            mSplitCipherReference,
            mCountDownLatch,
            true
        )

        mockStatic(SplitCipherFactory::class.java).use { mock ->
            mock.`when`<Any> {
                SplitCipherFactory.create(
                    any(), eq(SplitEncryptionLevel.AES_128_CBC)
                )
            }.thenReturn(mToCipher)

            encryptionMigrationTask.execute()

            mock.verify { SplitCipherFactory.create(mApiKey, SplitEncryptionLevel.AES_128_CBC) }
        }
    }

    @Test
    fun targetEncryptionLevelIsDeterminedWithEncryptionDisabledProperty() {
        val encryptionMigrationTask = EncryptionMigrationTask(
            mApiKey,
            mSplitDatabase,
            mSplitCipherReference,
            mCountDownLatch,
            false
        )

        mockStatic(SplitCipherFactory::class.java).use { mock ->
            mock.`when`<Any> {
                SplitCipherFactory.create(
                    any(), eq(SplitEncryptionLevel.NONE)
                )
            }.thenReturn(mToCipher)

            encryptionMigrationTask.execute()

            mock.verify { SplitCipherFactory.create(mApiKey, SplitEncryptionLevel.NONE) }
        }
    }

    @Test
    fun toCipherIsSetToCipherReference() {
        val encryptionMigrationTask = EncryptionMigrationTask(
            mApiKey,
            mSplitDatabase,
            mSplitCipherReference,
            mCountDownLatch,
            false
        )

        mockStatic(SplitCipherFactory::class.java).use { mock ->
            mock.`when`<Any> {
                SplitCipherFactory.create(
                    any(), eq(SplitEncryptionLevel.NONE)
                )
            }.thenReturn(mToCipher)

            encryptionMigrationTask.execute()

            verify(mSplitCipherReference).set(mToCipher)
        }
    }

    @Test
    fun countDownLatchIsCountedDownAfterReferenceIsSet() {
        val encryptionMigrationTask = EncryptionMigrationTask(
            mApiKey,
            mSplitDatabase,
            mSplitCipherReference,
            mCountDownLatch,
            true
        )

        mockStatic(SplitCipherFactory::class.java).use { mock ->
            mock.`when`<Any> {
                SplitCipherFactory.create(
                    any(), eq(SplitEncryptionLevel.AES_128_CBC)
                )
            }.thenReturn(mToCipher)

            encryptionMigrationTask.execute()

            verify(mSplitCipherReference).set(mToCipher)
            assertEquals(0L, mCountDownLatch.count)
        }
    }

    @Test
    fun levelIsUpdatedInGeneralInfo() {
        EncryptionMigrationTask(
            mApiKey,
            mSplitDatabase,
            mSplitCipherReference,
            mCountDownLatch,
            true
        ).execute()

        verify(mGeneralInfoDao).update(argThat {
            it.name == "databaseEncryptionMode" && it.stringValue == "AES_128_CBC"
        })
    }

    @Test
    fun successfulExecutionReturnsSuccessTaskInfo() {
        val encryptionMigrationTask = EncryptionMigrationTask(
            mApiKey,
            mSplitDatabase,
            mSplitCipherReference,
            mCountDownLatch,
            true
        )

        val taskInfo = encryptionMigrationTask.execute()

        assertEquals(SplitTaskExecutionStatus.SUCCESS, taskInfo.status)
        assertEquals(SplitTaskType.GENERIC_TASK, taskInfo.taskType)
    }

    @Test
    fun unsuccessfulExecutionReturnsErrorTaskInfo() {
        val encryptionMigrationTask = EncryptionMigrationTask(
            mApiKey,
            mSplitDatabase,
            mSplitCipherReference,
            mCountDownLatch,
            true
        )

        mockStatic(SplitCipherFactory::class.java).use { mock ->
            mock.`when`<Any> {
                SplitCipherFactory.create(
                    any(), eq(SplitEncryptionLevel.AES_128_CBC)
                )
            }.thenThrow(RuntimeException("Test exception"))

            val taskInfo = encryptionMigrationTask.execute()

            assertEquals(SplitTaskExecutionStatus.ERROR, taskInfo.status)
            assertEquals(SplitTaskType.GENERIC_TASK, taskInfo.taskType)
        }
    }
}
