package io.split.android.client.storage.cipher

import io.split.android.client.service.executor.SplitTaskExecutionInfo
import io.split.android.client.service.executor.SplitTaskType
import io.split.android.client.storage.cipher.DBCipher.TaskProvider
import io.split.android.client.storage.db.SplitRoomDatabase
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class DBCipherTest {

    @Mock
    private lateinit var splitDatabase: SplitRoomDatabase

    @Mock
    private lateinit var toCipher: SplitCipher

    @Mock
    private lateinit var applyCipherTask: ApplyCipherTask

    @Mock
    private lateinit var taskProvider: TaskProvider
    private lateinit var apiKey: String

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        apiKey = "abcdefghijklmnopqrstuvwxyz"
        `when`(applyCipherTask.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK))
        `when`(taskProvider.get(eq(splitDatabase), any(), eq(toCipher))).thenReturn(applyCipherTask)
    }

    @Test
    fun testApplyCipherWhenMustApply() {
        val fromLevel: SplitEncryptionLevel = SplitEncryptionLevel.AES_128_CBC
        val toLevel = SplitEncryptionLevel.NONE

        DBCipher(splitDatabase, apiKey, toCipher, fromLevel, toLevel, taskProvider).apply()

        verify(applyCipherTask).execute()
    }

    @Test
    fun testApplyCipherWhenNoNeedToApply() {
        val fromLevel = SplitEncryptionLevel.NONE
        val toLevel = SplitEncryptionLevel.NONE

        DBCipher(splitDatabase, apiKey, toCipher, fromLevel, toLevel, taskProvider).apply()

        verify(applyCipherTask, never()).execute()
    }
}
