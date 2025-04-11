package io.split.android.client.storage.cipher

import io.split.android.client.service.executor.SplitTaskExecutionStatus
import io.split.android.client.service.executor.SplitTaskType
import io.split.android.client.storage.db.EventDao
import io.split.android.client.storage.db.EventEntity
import io.split.android.client.storage.db.GeneralInfoDao
import io.split.android.client.storage.db.GeneralInfoEntity
import io.split.android.client.storage.db.ImpressionDao
import io.split.android.client.storage.db.ImpressionEntity
import io.split.android.client.storage.db.ImpressionsCountDao
import io.split.android.client.storage.db.ImpressionsCountEntity
import io.split.android.client.storage.db.MyLargeSegmentDao
import io.split.android.client.storage.db.MyLargeSegmentEntity
import io.split.android.client.storage.db.MySegmentDao
import io.split.android.client.storage.db.MySegmentEntity
import io.split.android.client.storage.db.SplitDao
import io.split.android.client.storage.db.SplitEntity
import io.split.android.client.storage.db.SplitRoomDatabase
import io.split.android.client.storage.db.attributes.AttributesDao
import io.split.android.client.storage.db.attributes.AttributesEntity
import io.split.android.client.storage.db.impressions.unique.UniqueKeyEntity
import io.split.android.client.storage.db.impressions.unique.UniqueKeysDao
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mock
import org.mockito.Mockito.times
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
    private lateinit var myLargeSegmentDao: MyLargeSegmentDao

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

    @Mock
    private lateinit var generalInfoDao: GeneralInfoDao

    private lateinit var applyCipherTask: ApplyCipherTask

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(splitDatabase.splitDao()).thenReturn(splitDao)
        `when`(splitDatabase.mySegmentDao()).thenReturn(mySegmentDao)
        `when`(splitDatabase.myLargeSegmentDao()).thenReturn(myLargeSegmentDao)
        `when`(splitDatabase.impressionDao()).thenReturn(impressionDao)
        `when`(splitDatabase.eventDao()).thenReturn(eventDao)
        `when`(splitDatabase.impressionsCountDao()).thenReturn(impressionsCountDao)
        `when`(splitDatabase.uniqueKeysDao()).thenReturn(uniqueKeysDao)
        `when`(splitDatabase.attributesDao()).thenReturn(attributesDao)
        `when`(splitDatabase.generalInfoDao()).thenReturn(generalInfoDao)

        `when`(fromCipher.decrypt(anyString())).thenAnswer { invocation -> "decrypted_${invocation.arguments[0]}" }
        `when`(toCipher.encrypt(anyString())).thenAnswer { invocation -> "encrypted_${invocation.arguments[0]}" }
        `when`(splitDatabase.runInTransaction(any())).thenAnswer { invocation -> (invocation.arguments[0] as Runnable).run() }

        applyCipherTask = ApplyCipherTask(splitDatabase, fromCipher, toCipher)
    }

    @Test
    fun testExecute() {
        val result = applyCipherTask.execute()

        assertEquals(SplitTaskType.GENERIC_TASK, result.taskType)
        assertEquals(SplitTaskExecutionStatus.SUCCESS, result.status)
    }

    @Test
    fun `flags are migrated`() {
        `when`(splitDao.all).thenReturn(
            listOf(
                SplitEntity().apply { name = "name1"; body = "body1" },
                SplitEntity().apply { name = "name2"; body = "body2" },
            )
        )

        applyCipherTask.execute()

        verify(splitDao).all
        verify(fromCipher).decrypt("name1")
        verify(fromCipher).decrypt("body1")
        verify(toCipher).encrypt("decrypted_name1")
        verify(toCipher).encrypt("decrypted_body1")
        verify(splitDao).update("name1", "encrypted_decrypted_name1", "encrypted_decrypted_body1")

        verify(fromCipher).decrypt("name2")
        verify(fromCipher).decrypt("body2")
        verify(toCipher).encrypt("decrypted_name2")
        verify(toCipher).encrypt("decrypted_body2")
        verify(splitDao).update("name2", "encrypted_decrypted_name2", "encrypted_decrypted_body2")
    }

    @Test
    fun `traffic types are migrated`() {
        `when`(generalInfoDao.getByName(GeneralInfoEntity.TRAFFIC_TYPES_MAP)).thenReturn(
            GeneralInfoEntity(GeneralInfoEntity.TRAFFIC_TYPES_MAP, "trafficTypesMap")
        )

        applyCipherTask.execute()

        verify(generalInfoDao).getByName(GeneralInfoEntity.TRAFFIC_TYPES_MAP)
        verify(fromCipher).decrypt("trafficTypesMap")
        verify(toCipher).encrypt("decrypted_trafficTypesMap")
        verify(generalInfoDao).update(argThat {
            it.stringValue.equals("encrypted_decrypted_trafficTypesMap")
        })
    }

    @Test
    fun `flag sets are migrated`() {
        `when`(generalInfoDao.getByName(GeneralInfoEntity.FLAG_SETS_MAP)).thenReturn(
            GeneralInfoEntity(GeneralInfoEntity.FLAG_SETS_MAP, "flagSetsMap")
        )

        applyCipherTask.execute()

        verify(generalInfoDao).getByName(GeneralInfoEntity.FLAG_SETS_MAP)
        verify(fromCipher).decrypt("flagSetsMap")
        verify(toCipher).encrypt("decrypted_flagSetsMap")
        verify(generalInfoDao).update(argThat {
            it.stringValue.equals("encrypted_decrypted_flagSetsMap")
        })
    }

    @Test
    fun `segments are migrated`() {
        `when`(mySegmentDao.all).thenReturn(
            listOf(
                MySegmentEntity.creator().createEntity("userKey1", "segment1,segment2", 999999),
                MySegmentEntity.creator().createEntity("userKey2", "segment3,segment4", 999999),
            )
        )

        applyCipherTask.execute()

        verify(mySegmentDao).all
        verify(fromCipher).decrypt("userKey1")
        verify(fromCipher).decrypt("segment1,segment2")
        verify(toCipher).encrypt("decrypted_userKey1")
        verify(toCipher).encrypt("decrypted_segment1,segment2")
        verify(mySegmentDao).update(
            "userKey1",
            "encrypted_decrypted_userKey1",
            "encrypted_decrypted_segment1,segment2"
        )

        verify(fromCipher).decrypt("userKey2")
        verify(fromCipher).decrypt("segment3,segment4")
        verify(toCipher).encrypt("decrypted_userKey2")
        verify(toCipher).encrypt("decrypted_segment3,segment4")
        verify(mySegmentDao).update(
            "userKey2",
            "encrypted_decrypted_userKey2",
            "encrypted_decrypted_segment3,segment4"
        )
    }

    @Test
    fun `large segments are migrated`() {
        `when`(myLargeSegmentDao.all).thenReturn(
            listOf(
                MyLargeSegmentEntity.creator()
                    .createEntity("userKey1", "segment1,segment2", 999999),
                MyLargeSegmentEntity.creator()
                    .createEntity("userKey2", "segment3,segment4", 999999),
            )
        )

        applyCipherTask.execute()

        verify(myLargeSegmentDao).all
        verify(fromCipher).decrypt("userKey1")
        verify(fromCipher).decrypt("segment1,segment2")
        verify(toCipher).encrypt("decrypted_userKey1")
        verify(toCipher).encrypt("decrypted_segment1,segment2")
        verify(myLargeSegmentDao).update(
            "userKey1",
            "encrypted_decrypted_userKey1",
            "encrypted_decrypted_segment1,segment2"
        )

        verify(fromCipher).decrypt("userKey2")
        verify(fromCipher).decrypt("segment3,segment4")
        verify(toCipher).encrypt("decrypted_userKey2")
        verify(toCipher).encrypt("decrypted_segment3,segment4")
        verify(myLargeSegmentDao).update(
            "userKey2",
            "encrypted_decrypted_userKey2",
            "encrypted_decrypted_segment3,segment4"
        )
    }

    @Test
    fun `impressions are migrated`() {
        `when`(impressionDao.all).thenReturn(
            listOf(
                ImpressionEntity().apply { id = 1; testName = "test1"; body = "body1" },
                ImpressionEntity().apply { id = 2; testName = "test2"; body = "body2" },
            )
        )

        applyCipherTask.execute()

        verify(impressionDao).all
        verify(fromCipher, times(0)).decrypt("1")
        verify(fromCipher).decrypt("test1")
        verify(fromCipher).decrypt("body1")
        verify(toCipher).encrypt("decrypted_test1")
        verify(toCipher).encrypt("decrypted_body1")
        verify(impressionDao).insert(argThat(ArgumentMatcher<ImpressionEntity> { entity ->
            entity.id == 1L && entity.testName == "encrypted_decrypted_test1" && entity.body == "encrypted_decrypted_body1"
        }))

        verify(fromCipher, times(0)).decrypt("2")
        verify(fromCipher).decrypt("test2")
        verify(fromCipher).decrypt("body2")
        verify(toCipher).encrypt("decrypted_test2")
        verify(toCipher).encrypt("decrypted_body2")
        verify(impressionDao).insert(argThat(ArgumentMatcher<ImpressionEntity> { entity ->
            entity.id == 2L && entity.testName == "encrypted_decrypted_test2" && entity.body == "encrypted_decrypted_body2"
        }))
    }

    @Test
    fun `events are migrated`() {
        `when`(eventDao.all).thenReturn(
            listOf(
                EventEntity().apply { id = 1; body = "body1"; createdAt = 999991 },
                EventEntity().apply { id = 2; body = "body2"; createdAt = 999992 },
            )
        )

        applyCipherTask.execute()

        verify(eventDao).all
        verify(fromCipher, times(0)).decrypt("1")
        verify(fromCipher).decrypt("body1")
        verify(toCipher).encrypt("decrypted_body1")
        verify(eventDao).insert(argThat(ArgumentMatcher<EventEntity> { entity ->
            entity.id == 1.toLong() && entity.body == "encrypted_decrypted_body1" && entity.createdAt == 999991L
        }))

        verify(fromCipher, times(0)).decrypt("2")
        verify(fromCipher).decrypt("body2")
        verify(toCipher).encrypt("decrypted_body2")
        verify(eventDao).insert(argThat(ArgumentMatcher<EventEntity> { entity ->
            entity.id == 2.toLong() && entity.body == "encrypted_decrypted_body2" && entity.createdAt == 999992L
        }))
    }

    @Test
    fun `impressions count are migrated`() {
        `when`(impressionsCountDao.all).thenReturn(
            listOf(
                ImpressionsCountEntity().apply { id = 1; body = "body1"; createdAt = 999991 },
                ImpressionsCountEntity().apply { id = 2; body = "body2"; createdAt = 999992 },
            )
        )

        applyCipherTask.execute()

        verify(impressionsCountDao).all
        verify(fromCipher, times(0)).decrypt("1")
        verify(fromCipher).decrypt("body1")
        verify(toCipher).encrypt("decrypted_body1")
        verify(impressionsCountDao).insert(argThat(ArgumentMatcher<ImpressionsCountEntity> { entity ->
            entity.id == 1.toLong() && entity.body == "encrypted_decrypted_body1" && entity.createdAt == 999991L
        }))

        verify(fromCipher, times(0)).decrypt("2")
        verify(fromCipher).decrypt("body2")
        verify(toCipher).encrypt("decrypted_body2")
        verify(impressionsCountDao).insert(argThat(ArgumentMatcher<ImpressionsCountEntity> { entity ->
            entity.id == 2.toLong() && entity.body == "encrypted_decrypted_body2" && entity.createdAt == 999992L
        }))
    }

    @Test
    fun `unique keys are migrated`() {
        `when`(uniqueKeysDao.all).thenReturn(
            listOf(
                UniqueKeyEntity().apply { id = 1; userKey = "key1"; featureList = "feature1,feature2"; createdAt = 999991 },
                UniqueKeyEntity().apply { id = 2; userKey = "key2"; featureList = "feature3,feature4"; createdAt = 999992 },
            )
        )

        applyCipherTask.execute()

        verify(uniqueKeysDao).all
        verify(fromCipher, times(0)).decrypt("1")
        verify(fromCipher).decrypt("key1")
        verify(fromCipher).decrypt("feature1,feature2")
        verify(toCipher).encrypt("decrypted_key1")
        verify(toCipher).encrypt("decrypted_feature1,feature2")
        verify(uniqueKeysDao).insert(argThat(ArgumentMatcher<UniqueKeyEntity> { entity ->
            entity.id == 1.toLong() && entity.userKey == "encrypted_decrypted_key1" && entity.featureList == "encrypted_decrypted_feature1,feature2" && entity.createdAt == 999991L
        }))

        verify(fromCipher, times(0)).decrypt("2")
        verify(fromCipher).decrypt("key2")
        verify(fromCipher).decrypt("feature3,feature4")
        verify(toCipher).encrypt("decrypted_key2")
        verify(toCipher).encrypt("decrypted_feature3,feature4")
        verify(uniqueKeysDao).insert(argThat(ArgumentMatcher<UniqueKeyEntity> { entity ->
            entity.id == 2.toLong() && entity.userKey == "encrypted_decrypted_key2" && entity.featureList == "encrypted_decrypted_feature3,feature4" && entity.createdAt == 999992L
        }))
    }

    @Test
    fun `attributes are migrated`() {
        `when`(attributesDao.all).thenReturn(
            listOf(
                AttributesEntity().apply { userKey = "key1"; attributes = "{\"attr1\":\"val1\",\"attr2\":\"val2\"}"; updatedAt = 999991 },
                AttributesEntity().apply { userKey = "key2"; attributes = "{\"attr3\":\"val3\",\"attr4\":\"val4\"}"; updatedAt = 999992 },
            ))

        applyCipherTask.execute()

        verify(attributesDao).all
        verify(fromCipher).decrypt("key1")
        verify(fromCipher).decrypt("{\"attr1\":\"val1\",\"attr2\":\"val2\"}")
        verify(toCipher).encrypt("decrypted_key1")
        verify(toCipher).encrypt("decrypted_{\"attr1\":\"val1\",\"attr2\":\"val2\"}")
        verify(attributesDao).update("key1", "encrypted_decrypted_key1", "encrypted_decrypted_{\"attr1\":\"val1\",\"attr2\":\"val2\"}")

        verify(fromCipher).decrypt("key2")
        verify(fromCipher).decrypt("{\"attr3\":\"val3\",\"attr4\":\"val4\"}")
        verify(toCipher).encrypt("decrypted_key2")
        verify(toCipher).encrypt("decrypted_{\"attr3\":\"val3\",\"attr4\":\"val4\"}")
        verify(attributesDao).update("key2", "encrypted_decrypted_key2", "encrypted_decrypted_{\"attr3\":\"val3\",\"attr4\":\"val4\"}")
    }
}
