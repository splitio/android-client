package io.split.android.client.storage.splits;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.dtos.Split;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.GeneralInfoDao;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.RefusedWrites;
import io.split.android.client.storage.db.SplitDao;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitQueryDao;
import io.split.android.client.storage.db.SplitRoomDatabase;

public class SqLitePersistentSplitsStorageTest {

    @Mock
    private SplitRoomDatabase mDatabase;
    @Mock
    private SplitListTransformer<SplitEntity, Split> mEntityToSplitTransformer;
    @Mock
    private SplitListTransformer<Split, SplitEntity> mSplitToSplitEntityTransformer;
    @Mock
    private SplitDao mSplitDao;
    @Mock
    private SplitQueryDao mSplitQueryDao;
    @Mock
    private SplitCipher mCipher;
    private final GeneralInfoDao mGeneralInfoDao = mock(GeneralInfoDao.class);
    private SqLitePersistentSplitsStorage mStorage;
    private AutoCloseable mAutoCloseable;
    private final Map<String, Set<String>> mFlagSets = new HashMap<>();
    private final Map<String, Integer> mTrafficTypes = new HashMap<>();

    @Before
    public void setUp() {
        mAutoCloseable = MockitoAnnotations.openMocks(this);
        when(mDatabase.generalInfoDao()).thenReturn(mGeneralInfoDao);
        when(mDatabase.splitDao()).thenReturn(mSplitDao);
        when(mDatabase.getSplitQueryDao()).thenReturn(mSplitQueryDao);
        doAnswer((Answer<Void>) invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(mDatabase).runInTransaction(any(Runnable.class));
        instantiateStorage(invocation -> invocation.getArgument(0));
    }

    private void instantiateStorage(Answer<String> encryptionAnswer) {
        when(mCipher.encrypt(any())).thenAnswer(encryptionAnswer);
        mStorage = new SqLitePersistentSplitsStorage(mDatabase, mEntityToSplitTransformer, mSplitToSplitEntityTransformer, mCipher);
    }

    @After
    public void tearDown() throws Exception {
        RefusedWrites.resetForTesting();
        mAutoCloseable.close();
    }

    @Test
    public void getAllUsesTransformer() {
        List<SplitEntity> mockEntities = getMockEntities();
        Map<String, SplitEntity> map = new HashMap<>();
        for (SplitEntity entity : mockEntities) {
            map.put(entity.getName(), entity);
        }

        when(mSplitDao.getAll()).thenReturn(mockEntities);
        when(mDatabase.splitDao()).thenReturn(mSplitDao);
        SplitQueryDao queryDao = mock(SplitQueryDao.class);
        when(queryDao.getAllAsMap()).thenReturn(map);
        when(mDatabase.getSplitQueryDao()).thenReturn(queryDao);

        mStorage.getAll();

        verify(mEntityToSplitTransformer).transform(map);
    }

    @Test
    public void updateFlagsSpecUsesGeneralInfoDao() {
        GeneralInfoDao generalInfoDao = mock(GeneralInfoDao.class);
        when(mDatabase.generalInfoDao()).thenReturn(generalInfoDao);
        mStorage.updateFlagsSpec("2.5");

        verify(generalInfoDao).update(argThat(entity -> entity.getName().equals("flagsSpec") &&
                entity.getStringValue().equals("2.5")));
    }

    @Test
    public void getFlagsSpecFetchesValueFromGeneralInfoDao() {
        GeneralInfoDao generalInfoDao = mock(GeneralInfoDao.class);
        when(mDatabase.generalInfoDao()).thenReturn(generalInfoDao);
        when(generalInfoDao.getByName("flagsSpec")).thenReturn(new GeneralInfoEntity("flagsSpec", "2.5"));

        String flagsSpec = mStorage.getFlagsSpec();

        assertEquals("2.5", flagsSpec);
    }

    @Test
    public void getFlagsSpecReturnsNullWhenItIsNotSet() {
        GeneralInfoDao generalInfoDao = mock(GeneralInfoDao.class);
        when(mDatabase.generalInfoDao()).thenReturn(generalInfoDao);
        when(generalInfoDao.getByName("flagsSpec")).thenReturn(null);

        String flagsSpec = mStorage.getFlagsSpec();

        assertNull(flagsSpec);
    }

    @Test
    public void updateRemovesEncryptedSplitNames() {
        List<Split> activeSplits = Collections.emptyList();
        List<Split> archivedSplits = new ArrayList<>();
        long changeNumber = 9999;
        long timestamp = 123456789;

        Split split1 = new Split();
        split1.name = "split-1";
        archivedSplits.add(split1);
        Split split2 = new Split();
        split2.name = "split-2";
        archivedSplits.add(split2);
        Split split3 = new Split();
        split3.name = "split-3";
        archivedSplits.add(split3);
        ProcessedSplitChange change = new ProcessedSplitChange(activeSplits, archivedSplits, changeNumber, timestamp);
        when(mCipher.encrypt(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0) + "_encrypted");

        mStorage.update(change, mTrafficTypes, mFlagSets);

        verify(mSplitDao).delete(argThat(list -> list.contains("split-1_encrypted") && list.contains("split-2_encrypted") && list.contains("split-3_encrypted") && list.size() == 3));
    }

    @Test
    public void updateForSplitChangeUsesTransformer() {
        List<Split> activeSplits = new ArrayList<>();
        List<Split> archivedSplits = Collections.emptyList();
        long changeNumber = 9999;
        long timestamp = 123456789;

        Split split1 = new Split();
        split1.name = "split-1";
        activeSplits.add(split1);
        Split split2 = new Split();
        split2.name = "split-2";
        activeSplits.add(split2);
        Split split3 = new Split();
        split3.name = "split-3";
        activeSplits.add(split3);
        ProcessedSplitChange change = new ProcessedSplitChange(activeSplits, archivedSplits, changeNumber, timestamp);
        when(mCipher.encrypt(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0) + "_encrypted");

        mStorage.update(change, mTrafficTypes, mFlagSets);

        verify(mSplitToSplitEntityTransformer).transform(activeSplits);
    }

    @Test
    public void refusedWriteStoresMinusOneWhenAFlagFailsToEncrypt() {
        List<Split> activeSplits = new ArrayList<>();
        activeSplits.add(newSplit("split-1"));
        activeSplits.add(newSplit("split-2"));
        activeSplits.add(newSplit("split-3"));
        SplitEntity first = new SplitEntity();
        first.setName("split-1");
        SplitEntity second = new SplitEntity();
        second.setName("split-2");
        when(mSplitToSplitEntityTransformer.transform(activeSplits)).thenReturn(java.util.Arrays.asList(first, second));
        ProcessedSplitChange change = new ProcessedSplitChange(activeSplits, Collections.emptyList(), 9999, 123456789);

        boolean result = mStorage.update(change, mTrafficTypes, mFlagSets);

        assertFalse(result);
        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.CHANGE_NUMBER_INFO) &&
                entity.getLongValue() == -1));
        verify(mGeneralInfoDao, never()).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.CHANGE_NUMBER_INFO) &&
                entity.getLongValue() == 9999));
    }

    private static Split newSplit(String name) {
        Split split = new Split();
        split.name = name;
        return split;
    }

    @Test
    public void refusedWriteWhenArchivedNameFailsToEncrypt() {
        List<Split> archived = new ArrayList<>();
        archived.add(newSplit("split-1"));
        archived.add(newSplit("split-2"));
        when(mCipher.encrypt("split-1")).thenReturn(null);
        ProcessedSplitChange change = new ProcessedSplitChange(Collections.emptyList(), archived, 9999, 123456789);

        boolean result = mStorage.update(change, mTrafficTypes, mFlagSets);

        assertFalse(result);
        assertEquals(Collections.singletonList(-1L), changeNumbersWritten());
        verify(mSplitDao).delete(Collections.singletonList("split-2"));
    }

    @Test
    public void trafficTypesEncryptFailureWritesNoNullRowAndMinusOne() {
        mTrafficTypes.put("user", 1);
        String trafficTypesJson = "{\"user\":1}";
        when(mCipher.encrypt(trafficTypesJson)).thenReturn(null);
        ProcessedSplitChange change = new ProcessedSplitChange(Collections.emptyList(), Collections.emptyList(), 9999, 123456789);

        boolean result = mStorage.update(change, mTrafficTypes, mFlagSets);

        assertFalse(result);
        assertEquals(Collections.singletonList(-1L), changeNumbersWritten());
        assertNoNullMetadataRow();
        verify(mGeneralInfoDao, never()).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.TRAFFIC_TYPES_MAP)));
        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.FLAG_SETS_MAP)));
    }

    @Test
    public void flagSetsEncryptFailureWritesNoNullRowAndMinusOne() {
        mTrafficTypes.put("user", 1);
        when(mCipher.encrypt("{}")).thenReturn(null);
        ProcessedSplitChange change = new ProcessedSplitChange(Collections.emptyList(), Collections.emptyList(), 9999, 123456789);

        boolean result = mStorage.update(change, mTrafficTypes, mFlagSets);

        assertFalse(result);
        assertEquals(Collections.singletonList(-1L), changeNumbersWritten());
        assertNoNullMetadataRow();
        verify(mGeneralInfoDao, never()).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.FLAG_SETS_MAP)));
        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.TRAFFIC_TYPES_MAP)));
    }

    @Test
    public void encryptExceptionWritesOnlyMinusOne() {
        when(mCipher.encrypt("{}")).thenThrow(new IllegalStateException("keystore"));
        List<Split> active = Collections.singletonList(newSplit("split-1"));
        ProcessedSplitChange change = new ProcessedSplitChange(active, Collections.emptyList(), 9999, 123456789);

        boolean result = mStorage.update(change, mTrafficTypes, mFlagSets);

        assertFalse(result);
        verify(mGeneralInfoDao, times(1)).update(any());
        assertEquals(Collections.singletonList(-1L), changeNumbersWritten());
        verify(mDatabase, never()).runInTransaction(any(Runnable.class));
        assertTrue(RefusedWrites.flagsRefused(mDatabase));
    }

    @Test
    public void refusalIsStickyForTheDatabase() {
        mStorage.update(refusedChange(), mTrafficTypes, mFlagSets);
        SqLitePersistentSplitsStorage second = new SqLitePersistentSplitsStorage(
                mDatabase, mEntityToSplitTransformer, mSplitToSplitEntityTransformer, mCipher);
        ProcessedSplitChange fullySuccessful = new ProcessedSplitChange(Collections.emptyList(), Collections.emptyList(), 12345, 1);

        boolean result = second.update(fullySuccessful, mTrafficTypes, mFlagSets);

        assertFalse(result);
        assertEquals(java.util.Arrays.asList(-1L, -1L), changeNumbersWritten());
    }

    @Test
    public void refusalDoesNotReachAnotherDatabase() {
        mStorage.update(refusedChange(), mTrafficTypes, mFlagSets);
        SplitRoomDatabase otherDatabase = mock(SplitRoomDatabase.class);
        GeneralInfoDao otherGeneralInfoDao = mock(GeneralInfoDao.class);
        when(otherDatabase.generalInfoDao()).thenReturn(otherGeneralInfoDao);
        when(otherDatabase.splitDao()).thenReturn(mock(SplitDao.class));
        doAnswer((Answer<Void>) invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(otherDatabase).runInTransaction(any(Runnable.class));
        SqLitePersistentSplitsStorage other = new SqLitePersistentSplitsStorage(
                otherDatabase, mEntityToSplitTransformer, mSplitToSplitEntityTransformer, mCipher);
        ProcessedSplitChange change = new ProcessedSplitChange(Collections.emptyList(), Collections.emptyList(), 12345, 1);

        boolean result = other.update(change, mTrafficTypes, mFlagSets);

        assertTrue(result);
        verify(otherGeneralInfoDao).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.CHANGE_NUMBER_INFO) &&
                entity.getLongValue() == 12345));
    }

    @Test
    public void refusalIsReadInsideTheTransaction() {
        doAnswer((Answer<Void>) invocation -> {
            RefusedWrites.refuseFlags(mDatabase);
            RefusedWrites.refuseRuleBasedSegments(mDatabase);
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(mDatabase).runInTransaction(any(Runnable.class));
        ProcessedSplitChange change = new ProcessedSplitChange(Collections.emptyList(), Collections.emptyList(), 9999, 1);

        boolean result = mStorage.update(change, mTrafficTypes, mFlagSets);

        assertFalse(result);
        assertEquals(Collections.singletonList(-1L), changeNumbersWritten());
    }

    @Test
    public void migrationSkipsTrafficTypesRowWhenEncryptFails() {
        when(mCipher.encrypt("{\"user\":2}")).thenReturn(null);
        stubStoredActiveSplitsWithoutSets();

        mStorage.getSnapshot();

        verify(mGeneralInfoDao, never()).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.TRAFFIC_TYPES_MAP)));
        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.FLAG_SETS_MAP)));
    }

    @Test
    public void snapshotToleratesTrafficTypesRowWithNullValue() {
        when(mGeneralInfoDao.getByName(GeneralInfoEntity.TRAFFIC_TYPES_MAP))
                .thenReturn(new GeneralInfoEntity(GeneralInfoEntity.TRAFFIC_TYPES_MAP, (String) null));
        when(mGeneralInfoDao.getByName(GeneralInfoEntity.FLAG_SETS_MAP))
                .thenReturn(new GeneralInfoEntity(GeneralInfoEntity.FLAG_SETS_MAP, "{\"set_a\":[\"flag-1\"]}"));
        when(mCipher.decrypt(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0));

        SplitsSnapshot snapshot = mStorage.getSnapshot();

        assertEquals(Collections.emptyMap(), snapshot.getTrafficTypesMap());
        assertEquals(Collections.singletonMap("set_a", Collections.singleton("flag-1")), snapshot.getFlagSetsMap());
    }

    @Test
    public void snapshotToleratesFlagSetsRowWithNullValue() {
        when(mGeneralInfoDao.getByName(GeneralInfoEntity.TRAFFIC_TYPES_MAP))
                .thenReturn(new GeneralInfoEntity(GeneralInfoEntity.TRAFFIC_TYPES_MAP, "{\"user\":1}"));
        when(mGeneralInfoDao.getByName(GeneralInfoEntity.FLAG_SETS_MAP))
                .thenReturn(new GeneralInfoEntity(GeneralInfoEntity.FLAG_SETS_MAP, (String) null));
        when(mCipher.decrypt(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0));

        SplitsSnapshot snapshot = mStorage.getSnapshot();

        assertEquals(Collections.emptyMap(), snapshot.getFlagSetsMap());
        assertEquals(Collections.singletonMap("user", 1), snapshot.getTrafficTypesMap());
    }

    @Test
    public void snapshotRepairsTrafficTypesRowWithNullValueFromStoredSplits() {
        when(mGeneralInfoDao.getByName(GeneralInfoEntity.TRAFFIC_TYPES_MAP))
                .thenReturn(new GeneralInfoEntity(GeneralInfoEntity.TRAFFIC_TYPES_MAP, (String) null));
        stubStoredActiveSplitsWithoutSets();

        SplitsSnapshot snapshot = mStorage.getSnapshot();

        assertEquals(Collections.singletonMap("user", 2), snapshot.getTrafficTypesMap());
        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.TRAFFIC_TYPES_MAP) &&
                "{\"user\":2}".equals(entity.getStringValue())));
    }

    @Test
    public void snapshotRepairsFlagSetsRowWithNullValueFromStoredSplits() {
        when(mGeneralInfoDao.getByName(GeneralInfoEntity.TRAFFIC_TYPES_MAP))
                .thenReturn(new GeneralInfoEntity(GeneralInfoEntity.TRAFFIC_TYPES_MAP, "{\"user\":1}"));
        when(mGeneralInfoDao.getByName(GeneralInfoEntity.FLAG_SETS_MAP))
                .thenReturn(new GeneralInfoEntity(GeneralInfoEntity.FLAG_SETS_MAP, (String) null));
        List<Split> storedSplits = Collections.singletonList(new Split("flag-1",
                "{\"name\":\"flag-1\",\"status\":\"ACTIVE\",\"trafficTypeName\":\"user\",\"sets\":[\"set_a\"]}"));
        when(mEntityToSplitTransformer.transform(anyMap())).thenReturn(storedSplits);
        when(mCipher.decrypt(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0));

        SplitsSnapshot snapshot = mStorage.getSnapshot();

        assertEquals(Collections.singletonMap("set_a", Collections.singleton("flag-1")), snapshot.getFlagSetsMap());
        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.FLAG_SETS_MAP) &&
                "{\"set_a\":[\"flag-1\"]}".equals(entity.getStringValue())));
    }

    @Test
    public void snapshotRepairsTrafficTypesRowWithEmptyValueFromStoredSplits() {
        when(mGeneralInfoDao.getByName(GeneralInfoEntity.TRAFFIC_TYPES_MAP))
                .thenReturn(new GeneralInfoEntity(GeneralInfoEntity.TRAFFIC_TYPES_MAP, ""));
        stubStoredActiveSplitsWithoutSets();

        SplitsSnapshot snapshot = mStorage.getSnapshot();

        assertEquals(Collections.singletonMap("user", 2), snapshot.getTrafficTypesMap());
    }

    @Test
    public void snapshotRepairsTrafficTypesRowWithNullValueWhenFlagSetsRowIsValid() {
        when(mGeneralInfoDao.getByName(GeneralInfoEntity.TRAFFIC_TYPES_MAP))
                .thenReturn(new GeneralInfoEntity(GeneralInfoEntity.TRAFFIC_TYPES_MAP, (String) null));
        when(mGeneralInfoDao.getByName(GeneralInfoEntity.FLAG_SETS_MAP))
                .thenReturn(new GeneralInfoEntity(GeneralInfoEntity.FLAG_SETS_MAP, "{\"set_a\":[\"flag-1\"]}"));
        stubStoredActiveSplitsWithoutSets();

        SplitsSnapshot snapshot = mStorage.getSnapshot();

        assertEquals(Collections.singletonMap("user", 2), snapshot.getTrafficTypesMap());
        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.TRAFFIC_TYPES_MAP) &&
                "{\"user\":2}".equals(entity.getStringValue())));
    }

    @Test
    public void snapshotRepairsTrafficTypesRowWithEmptyValueWhenFlagSetsRowIsValid() {
        when(mGeneralInfoDao.getByName(GeneralInfoEntity.TRAFFIC_TYPES_MAP))
                .thenReturn(new GeneralInfoEntity(GeneralInfoEntity.TRAFFIC_TYPES_MAP, ""));
        when(mGeneralInfoDao.getByName(GeneralInfoEntity.FLAG_SETS_MAP))
                .thenReturn(new GeneralInfoEntity(GeneralInfoEntity.FLAG_SETS_MAP, "{\"set_a\":[\"flag-1\"]}"));
        stubStoredActiveSplitsWithoutSets();

        SplitsSnapshot snapshot = mStorage.getSnapshot();

        assertEquals(Collections.singletonMap("user", 2), snapshot.getTrafficTypesMap());
        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.TRAFFIC_TYPES_MAP) &&
                "{\"user\":2}".equals(entity.getStringValue())));
    }

    private ProcessedSplitChange refusedChange() {
        List<Split> archived = Collections.singletonList(newSplit("split-1"));
        when(mCipher.encrypt("split-1")).thenReturn(null);
        return new ProcessedSplitChange(Collections.emptyList(), archived, 9999, 1);
    }

    private List<Long> changeNumbersWritten() {
        ArgumentCaptor<GeneralInfoEntity> captor = ArgumentCaptor.forClass(GeneralInfoEntity.class);
        verify(mGeneralInfoDao, org.mockito.Mockito.atLeast(0)).update(captor.capture());
        List<Long> numbers = new ArrayList<>();
        for (GeneralInfoEntity entity : captor.getAllValues()) {
            if (entity.getName().equals(GeneralInfoEntity.CHANGE_NUMBER_INFO)) {
                numbers.add(entity.getLongValue());
            }
        }
        return numbers;
    }

    private void assertNoNullMetadataRow() {
        ArgumentCaptor<GeneralInfoEntity> captor = ArgumentCaptor.forClass(GeneralInfoEntity.class);
        verify(mGeneralInfoDao, org.mockito.Mockito.atLeast(0)).update(captor.capture());
        for (GeneralInfoEntity entity : captor.getAllValues()) {
            if (entity.getName().equals(GeneralInfoEntity.TRAFFIC_TYPES_MAP) || entity.getName().equals(GeneralInfoEntity.FLAG_SETS_MAP)) {
                assertNotNull(entity.getStringValue());
            }
        }
    }

    @Test
    public void updatingNullSplitChangeDoesNotInteractWithDatabase() {
        mStorage.update((ProcessedSplitChange) null, mTrafficTypes, mFlagSets);

        verifyNoInteractions(mSplitToSplitEntityTransformer);
        verifyNoInteractions(mCipher);
        verifyNoInteractions(mDatabase);
    }

    @Test
    public void deleteRemovesEncryptedSplitNames() {
        when(mCipher.encrypt(any())).then((Answer<String>) invocation -> invocation.getArgument(0) + "_encrypted");

        mStorage.delete(Collections.singletonList("split-1"));

        verify(mSplitDao).delete(Collections.singletonList("split-1_encrypted"));
    }

    @Test
    public void clearResetsChangeNumberAndRemovesAllFlags() {
        mStorage.clear();

        verify(mDatabase.generalInfoDao()).update(argThat(new ArgumentMatcher<GeneralInfoEntity>() {
            @Override
            public boolean matches(GeneralInfoEntity argument) {
                return argument.getName().equals(GeneralInfoEntity.CHANGE_NUMBER_INFO) && argument.getLongValue() == -1;
            }
        }));
        verify(mDatabase.splitDao()).deleteAll();
        verify(mDatabase.getSplitQueryDao()).invalidate();
    }

    @Test
    public void getFilterQueryStringReturnsNullWhenItIsNotSet() {
        when(mDatabase.generalInfoDao().getByName(GeneralInfoEntity.SPLITS_FILTER_QUERY_STRING)).thenReturn(null);

        String filterQueryString = mStorage.getFilterQueryString();

        assertNull(filterQueryString);
    }

    @Test
    public void getFilterQueryStringReturnsStringValueWhenSet() {
        when(mDatabase.generalInfoDao().getByName(GeneralInfoEntity.SPLITS_FILTER_QUERY_STRING)).thenReturn(new GeneralInfoEntity(GeneralInfoEntity.SPLITS_FILTER_QUERY_STRING, "filterQueryString"));

        String filterQueryString = mStorage.getFilterQueryString();

        assertEquals("filterQueryString", filterQueryString);
    }

    @Test
    public void updateFilterQueryStringUpdatesItInGeneralInfo() {
        mStorage.updateFilterQueryString("filterQueryString");

        verify(mGeneralInfoDao).update(argThat(new ArgumentMatcher<GeneralInfoEntity>() {
            @Override
            public boolean matches(GeneralInfoEntity argument) {
                return argument.getStringValue().equals("filterQueryString") && argument.getName().equals(GeneralInfoEntity.SPLITS_FILTER_QUERY_STRING);
            }
        }));
    }

    @Test
    public void updateSingleSplitUsesTransformer() {
        Split split = new Split();
        split.name = "split-1";
        SplitEntity entity = new SplitEntity();
        entity.setName("split-1");
        when(mSplitToSplitEntityTransformer.transform(Collections.singletonList(split))).thenReturn(Collections.singletonList(entity));

        mStorage.update(split);

        verify(mSplitToSplitEntityTransformer).transform(Collections.singletonList(split));
    }

    @Test
    public void updateWritesEmptyFlagSetsRow() {
        List<Split> activeSplits = Collections.emptyList();
        List<Split> archivedSplits = Collections.emptyList();
        ProcessedSplitChange change = new ProcessedSplitChange(activeSplits, archivedSplits, 9999, 123456789);

        mStorage.update(change, mTrafficTypes, mFlagSets);

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.FLAG_SETS_MAP) &&
                entity.getStringValue().equals("{}")));
    }

    @Test
    public void updateSkipsFlagSetsRowWhenEncryptFails() {
        when(mCipher.encrypt("{}")).thenReturn(null);
        List<Split> activeSplits = Collections.emptyList();
        List<Split> archivedSplits = Collections.emptyList();
        ProcessedSplitChange change = new ProcessedSplitChange(activeSplits, archivedSplits, 9999, 123456789);

        mStorage.update(change, mTrafficTypes, mFlagSets);

        verify(mGeneralInfoDao, never()).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.FLAG_SETS_MAP)));
        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.CHANGE_NUMBER_INFO) &&
                entity.getLongValue() == -1));
        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP) &&
                entity.getLongValue() == 123456789));
    }

    @Test
    public void updateSkipsTrafficTypesRowWhenThereAreNoTrafficTypes() {
        List<Split> activeSplits = Collections.emptyList();
        List<Split> archivedSplits = Collections.emptyList();
        ProcessedSplitChange change = new ProcessedSplitChange(activeSplits, archivedSplits, 9999, 123456789);

        mStorage.update(change, mTrafficTypes, mFlagSets);

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.CHANGE_NUMBER_INFO)));
        verify(mGeneralInfoDao, never()).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.TRAFFIC_TYPES_MAP)));
    }

    @Test
    public void migrationWritesEmptyFlagSetsRow() {
        stubStoredActiveSplitsWithoutSets();

        mStorage.getSnapshot();

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.FLAG_SETS_MAP) &&
                entity.getStringValue().equals("{}")));
    }

    @Test
    public void migrationSkipsFlagSetsRowWhenEncryptFails() {
        when(mCipher.encrypt("{}")).thenReturn(null);
        stubStoredActiveSplitsWithoutSets();

        mStorage.getSnapshot();

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.TRAFFIC_TYPES_MAP)));
        verify(mGeneralInfoDao, never()).update(argThat(entity -> entity.getName().equals(GeneralInfoEntity.FLAG_SETS_MAP)));
    }

    @Test
    public void secondLoadDoesNotMigrateForAccountWithoutFlagSets() {
        FakeGeneralInfoDao generalInfoDao = new FakeGeneralInfoDao();
        when(mDatabase.generalInfoDao()).thenReturn(generalInfoDao);
        stubStoredActiveSplitsWithoutSets();

        new SqLitePersistentSplitsStorage(mDatabase, mEntityToSplitTransformer, mSplitToSplitEntityTransformer, mCipher)
                .getSnapshot();
        assertNotNull(generalInfoDao.getByName(GeneralInfoEntity.TRAFFIC_TYPES_MAP));
        int writesAfterFirstLoad = generalInfoDao.writeCount();

        new SqLitePersistentSplitsStorage(mDatabase, mEntityToSplitTransformer, mSplitToSplitEntityTransformer, mCipher)
                .getSnapshot();

        assertEquals(writesAfterFirstLoad, generalInfoDao.writeCount());
    }

    private void stubStoredActiveSplitsWithoutSets() {
        List<Split> storedSplits = new ArrayList<>();
        storedSplits.add(new Split("flag-1", "{\"name\":\"flag-1\",\"status\":\"ACTIVE\",\"trafficTypeName\":\"user\"}"));
        storedSplits.add(new Split("flag-2", "{\"name\":\"flag-2\",\"status\":\"ACTIVE\",\"trafficTypeName\":\"user\"}"));
        when(mEntityToSplitTransformer.transform(anyMap())).thenReturn(storedSplits);
        when(mCipher.decrypt(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0));
    }

    private static class FakeGeneralInfoDao implements GeneralInfoDao {
        private final Map<String, GeneralInfoEntity> mRows = new HashMap<>();
        private int mWrites = 0;

        @Override
        public void update(GeneralInfoEntity info) {
            mRows.put(info.getName(), info);
            mWrites++;
        }

        @Override
        public GeneralInfoEntity getByName(String name) {
            return mRows.get(name);
        }

        int writeCount() {
            return mWrites;
        }
    }

    private List<SplitEntity> getMockEntities() {
        ArrayList<SplitEntity> entities = new ArrayList<>();
        String jsonTemplate = "{\"name\":\"%s\", \"changeNumber\": %d}";
        long initialChangeNumber = 9999;

        for (int i = 0; i < 3; i++) {
            String splitName = "split-" + i;
            SplitEntity entity = new SplitEntity();
            entity.setName(splitName);
            entity.setBody(String.format(jsonTemplate, splitName, initialChangeNumber - i));
            entities.add(entity);
        }

        return entities;
    }
}
