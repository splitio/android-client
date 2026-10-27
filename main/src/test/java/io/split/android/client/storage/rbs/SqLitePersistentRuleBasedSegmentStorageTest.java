package io.split.android.client.storage.rbs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static helper.TestingHelper.getFieldValue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashSet;
import java.util.Set;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.RefusedWrites;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentDao;
import io.split.android.client.storage.general.GeneralInfoStorage;

public class SqLitePersistentRuleBasedSegmentStorageTest {

    private SplitCipher mCipher;
    private SplitRoomDatabase mDatabase;
    private RuleBasedSegmentDao mDao;
    private GeneralInfoStorage mGeneralInfoStorage;

    private SqLitePersistentRuleBasedSegmentStorage storage;

    @Before
    public void setUp() {
        mCipher = mock(SplitCipher.class);
        mDatabase = mock(SplitRoomDatabase.class);
        mDao = mock(RuleBasedSegmentDao.class);
        mGeneralInfoStorage = mock(GeneralInfoStorage.class);
        when(mDatabase.ruleBasedSegmentDao()).thenReturn(mDao);

        storage = new SqLitePersistentRuleBasedSegmentStorage(mCipher, mDatabase, mGeneralInfoStorage);
    }

    @After
    public void tearDown() {
        RefusedWrites.resetForTesting();
    }

    @Test
    public void rbsTransactionExceptionWritesMinusOneAndRethrows() {
        IllegalStateException failure = new IllegalStateException("boom");
        doThrow(failure).when(mDatabase).runInTransaction(any(Updater.class));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> storage.update(new HashSet<>(), new HashSet<>(), 123L));

        assertSame(failure, thrown);
        verify(mGeneralInfoStorage).setRbsChangeNumber(-1);
        assertTrue(RefusedWrites.ruleBasedSegmentsRefused(mDatabase));
        assertFalse(RefusedWrites.flagsRefused(mDatabase));
    }

    @Test
    public void rbsRefusalIsStickyForTheDatabase() {
        doThrow(new IllegalStateException("boom")).when(mDatabase).runInTransaction(any(Updater.class));
        assertThrows(IllegalStateException.class, () -> storage.update(new HashSet<>(), new HashSet<>(), 1L));
        org.mockito.Mockito.reset(mDatabase);
        when(mDatabase.ruleBasedSegmentDao()).thenReturn(mDao);
        org.mockito.Mockito.doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(mDatabase).runInTransaction(any(Updater.class));
        GeneralInfoStorage secondInfo = mock(GeneralInfoStorage.class);
        SqLitePersistentRuleBasedSegmentStorage second = new SqLitePersistentRuleBasedSegmentStorage(mCipher, mDatabase, secondInfo);

        second.update(new HashSet<>(), new HashSet<>(), 500L);

        verify(secondInfo).setRbsChangeNumber(-1);
        verify(secondInfo, never()).setRbsChangeNumber(500L);
    }

    @Test
    public void getSnapshotBuildsAndRunsSnapshotLoaderInstanceInTransaction() {
        RuleBasedSegmentSnapshot expectedSnapshot = mock(RuleBasedSegmentSnapshot.class);
        when(mDatabase.runInTransaction((SnapshotLoader) any())).thenReturn(expectedSnapshot);

        RuleBasedSegmentSnapshot result = storage.getSnapshot();

        ArgumentCaptor<SnapshotLoader> captor = ArgumentCaptor.forClass(SnapshotLoader.class);
        verify(mDatabase).runInTransaction(captor.capture());
        SnapshotLoader snapshotLoader = captor.getValue();
        assertSame(mDao, getFieldValue(snapshotLoader, "mDao"));
        assertSame(mCipher, getFieldValue(snapshotLoader, "mCipher"));
        assertSame(mGeneralInfoStorage, getFieldValue(snapshotLoader, "mGeneralInfoStorage"));
        assertSame(expectedSnapshot, result);
    }

    @Test
    public void updateBuildsAndRunsUpdaterInstanceInTransaction() {
        Set<RuleBasedSegment> toAdd = new HashSet<>();
        Set<RuleBasedSegment> toRemove = new HashSet<>();
        long changeNumber = 123L;

        storage.update(toAdd, toRemove, changeNumber);

        ArgumentCaptor<Updater> captor = ArgumentCaptor.forClass(Updater.class);
        verify(mDatabase).runInTransaction(captor.capture());
        Updater updater = captor.getValue();
        assertSame(mCipher, getFieldValue(updater, "mCipher"));
        assertSame(mDao, getFieldValue(updater, "mDao"));
        assertSame(mGeneralInfoStorage, getFieldValue(updater, "mGeneralInfoStorage"));
        assertSame(toAdd, getFieldValue(updater, "mToAdd"));
        assertSame(toRemove, getFieldValue(updater, "mToRemove"));
        assertSame(changeNumber, getFieldValue(updater, "mChangeNumber"));
    }

    @Test
    public void clearBuildsAndRunsClearerInstanceInTransaction() {
        storage.clear();

        ArgumentCaptor<Clearer> captor = ArgumentCaptor.forClass(Clearer.class);
        verify(mDatabase).runInTransaction(captor.capture());
        Clearer clearer = captor.getValue();
        assertSame(mDao, getFieldValue(clearer, "mDao"));
        assertSame(mGeneralInfoStorage, getFieldValue(clearer, "mGeneralInfoStorage"));
    }

    @Test
    public void cipherCannotBeNull() {
        assertThrows(NullPointerException.class,
                () -> new SqLitePersistentRuleBasedSegmentStorage(null, mDatabase, mGeneralInfoStorage));
    }

    @Test
    public void databaseCannotBeNull() {
        assertThrows(NullPointerException.class,
                () -> new SqLitePersistentRuleBasedSegmentStorage(mCipher, null, mGeneralInfoStorage));
    }

    @Test
    public void generalInfoStorageCannotBeNull() {
        assertThrows(NullPointerException.class,
                () -> new SqLitePersistentRuleBasedSegmentStorage(mCipher, mDatabase, null));
    }
}
