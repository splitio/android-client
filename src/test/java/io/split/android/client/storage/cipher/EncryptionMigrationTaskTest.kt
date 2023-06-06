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
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations


class EncryptionMigrationTaskTest {

    private val mApiKey: String = "abcdedfghijklmnopqrstuvwxyz"

    @Mock
    private lateinit var mSplitDatabase: SplitRoomDatabase

    @Mock
    private lateinit var mGeneralInfoDao: GeneralInfoDao

    @Mock
    private lateinit var mToCipher: SplitCipher

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mSplitDatabase.generalInfoDao()).thenReturn(mGeneralInfoDao)
    }

    @Test
    fun fromEncryptionLevelIsRetrievedFromGeneralInfo() {
        val encryptionMigrationTask = EncryptionMigrationTask(
            mApiKey,
            mSplitDatabase,
            false,
            mToCipher
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
    fun targetEncryptionLevelIsDeterminedWithEncryptionDisabledProperty() {
        val encryptionMigrationTask = EncryptionMigrationTask(
            mApiKey,
            mSplitDatabase,
            false,
            mToCipher
        )

        encryptionMigrationTask.execute()

        verify(mGeneralInfoDao).update(argThat { entity ->
            entity.name == "databaseEncryptionMode" && entity.stringValue == "NONE"
        })
    }

    @Test
    fun levelIsUpdatedInGeneralInfo() {
        EncryptionMigrationTask(
            mApiKey,
            mSplitDatabase,
            true,
            mToCipher
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
            true,
            mToCipher
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
            true,
            mToCipher
        )
        `when`(mGeneralInfoDao.update(any())).thenThrow(RuntimeException())

        val taskInfo = encryptionMigrationTask.execute()

        assertEquals(SplitTaskExecutionStatus.ERROR, taskInfo.status)
        assertEquals(SplitTaskType.GENERIC_TASK, taskInfo.taskType)
    }
}
