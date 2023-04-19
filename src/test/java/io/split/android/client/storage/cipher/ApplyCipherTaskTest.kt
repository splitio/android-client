package io.split.android.client.storage.cipher

import io.split.android.client.service.executor.SplitTaskExecutionStatus
import io.split.android.client.service.executor.SplitTaskType
import io.split.android.client.storage.db.EventDao
import io.split.android.client.storage.db.ImpressionDao
import io.split.android.client.storage.db.ImpressionsCountDao
import io.split.android.client.storage.db.MySegmentDao
import io.split.android.client.storage.db.SplitDao
import io.split.android.client.storage.db.SplitRoomDatabase
import io.split.android.client.storage.db.attributes.AttributesDao
import io.split.android.client.storage.db.impressions.unique.UniqueKeysDao
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class ApplyCipherTaskTest {

    @Mock
    private lateinit var splitDatabase: SplitRoomDatabase
    @Mock
    private lateinit var fromCipher: SplitCipher
    @Mock
    private lateinit var toCipher: SplitCipher

    @Mock
    private lateinit var splitDao: SplitDao
    @Mock
    private lateinit var mySegmentDao: MySegmentDao
    @Mock
    private lateinit var impressionDao: ImpressionDao
    @Mock
    private lateinit var eventDao: EventDao
    @Mock
    private lateinit var impressionsCountDao: ImpressionsCountDao
    @Mock
    private lateinit var uniqueKeysDao: UniqueKeysDao
    @Mock
    private lateinit var attributesDao: AttributesDao

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun testExecute() {
        `when`(splitDatabase.splitDao()).thenReturn(splitDao)
        `when`(splitDatabase.mySegmentDao()).thenReturn(mySegmentDao)
        `when`(splitDatabase.impressionDao()).thenReturn(impressionDao)
        `when`(splitDatabase.eventDao()).thenReturn(eventDao)
        `when`(splitDatabase.impressionsCountDao()).thenReturn(impressionsCountDao)
        `when`(splitDatabase.uniqueKeysDao()).thenReturn(uniqueKeysDao)
        `when`(splitDatabase.attributesDao()).thenReturn(attributesDao)

        `when`(fromCipher.decrypt(anyString())).thenAnswer { invocation -> invocation.arguments[0] }
        `when`(toCipher.encrypt(anyString())).thenAnswer { invocation -> invocation.arguments[0] }

        val applyCipherTask = ApplyCipherTask(splitDatabase, fromCipher, toCipher)
        val result = applyCipherTask.execute()

        assertEquals(SplitTaskType.GENERIC_TASK, result.taskType)
        assertEquals(SplitTaskExecutionStatus.SUCCESS, result.status)
    }
}
