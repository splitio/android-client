package io.split.android.client.storage.splits;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
    private SqLitePersistentSplitsStorage mStorage;
    private AutoCloseable mAutoCloseable;
    private final Map<String, Set<String>> mFlagSets = new HashMap<>();
    private final Map<String, Integer> mTrafficTypes = new HashMap<>();

    @Before
    public void setUp() {
        mAutoCloseable = MockitoAnnotations.openMocks(this);
        when(mDatabase.generalInfoDao()).thenReturn(mock(GeneralInfoDao.class));
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
        GeneralInfoDao generalInfoDao = mock(GeneralInfoDao.class);
        when(mDatabase.generalInfoDao()).thenReturn(generalInfoDao);

        mStorage.updateFilterQueryString("filterQueryString");

        verify(generalInfoDao).update(argThat(new ArgumentMatcher<GeneralInfoEntity>() {
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
