package io.split.android.client.storage.rbs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.split.android.client.storage.rbs.RuleBasedSegmentStorageImplTest.createRuleBasedSegment;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.RefusedWrites;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentDao;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentEntity;
import io.split.android.client.storage.general.GeneralInfoStorage;

public class UpdaterTest {

    private SplitCipher mCipher;
    private RuleBasedSegmentDao mDao;
    private GeneralInfoStorage mGeneralInfoStorage;
    private SplitRoomDatabase mDatabase;
    private Updater mUpdater;

    @Before
    public void setUp() {
        mCipher = mock(SplitCipher.class);
        mDao = mock(RuleBasedSegmentDao.class);
        mGeneralInfoStorage = mock(GeneralInfoStorage.class);
        mDatabase = mock(SplitRoomDatabase.class);
    }

    @After
    public void tearDown() {
        RefusedWrites.resetForTesting();
    }

    @Test
    public void runEncryptsRemovedSegmentNamesBeforeSendingToDao() {
        Set<RuleBasedSegment> toRemove = Set.of(
                createRuleBasedSegment("segment1"), createRuleBasedSegment("segment2"));
        when(mCipher.encrypt(any())).thenAnswer(invocation -> "encrypted_" + invocation.getArgument(0));
        mUpdater = createUpdater(Collections.emptySet(), toRemove, 10);

        mUpdater.run();

        verify(mCipher).encrypt("segment1");
        verify(mCipher).encrypt("segment2");
        verify(mDao).delete(argThat(new ArgumentMatcher<List<String>>() {
            @Override
            public boolean matches(List<String> argument) {
                return argument.size() == 2 &&
                        argument.contains("encrypted_segment1") &&
                        argument.contains("encrypted_segment2");
            }
        }));
    }

    @Test
    public void runEncryptsAddedSegmentNamesBeforeSendingToDao() {
        Set<RuleBasedSegment> toAdd = Set.of(
                createRuleBasedSegment("segment1"), createRuleBasedSegment("segment2"));
        when(mCipher.encrypt(any())).thenAnswer(invocation -> "encrypted_" + invocation.getArgument(0));
        mUpdater = createUpdater(toAdd, Collections.emptySet(), 10);

        mUpdater.run();

        verify(mCipher).encrypt("segment1");
        verify(mCipher).encrypt("segment2");
        verify(mDao).insert(argThat(new ArgumentMatcher<List<RuleBasedSegmentEntity>>() {
            @Override
            public boolean matches(List<RuleBasedSegmentEntity> argument) {
                argument.sort(Comparator.comparing(RuleBasedSegmentEntity::getName));
                RuleBasedSegmentEntity ruleBasedSegmentEntity = argument.get(0);
                RuleBasedSegmentEntity ruleBasedSegmentEntity1 = argument.get(1);
                return argument.size() == 2 &&
                        ruleBasedSegmentEntity.getName().equals("encrypted_segment1") &&
                        ruleBasedSegmentEntity1.getName().equals("encrypted_segment2") &&
                        ruleBasedSegmentEntity.getBody().startsWith("encrypted_") &&
                        ruleBasedSegmentEntity1.getBody().startsWith("encrypted_");
            }
        }));
    }

    @Test
    public void runUpdatesChangeNumber() {
        mUpdater = createUpdater(Collections.emptySet(), Collections.emptySet(), 10);

        mUpdater.run();

        verify(mGeneralInfoStorage).setRbsChangeNumber(10);
    }

    @Test
    public void runDoesNotUpdateSegmentIfEncryptedNameIsNull() {
        Set<RuleBasedSegment> toAdd = Set.of(
                createRuleBasedSegment("segment1"), createRuleBasedSegment("segment2"));
        Set<RuleBasedSegment> toRemove = Set.of(
                createRuleBasedSegment("segment3"), createRuleBasedSegment("segment4"));
        when(mCipher.encrypt(anyString())).thenReturn(null);
        when(mCipher.encrypt(argThat(argument -> argument.contains("segment1")))).thenReturn("encrypted_segment1");
        when(mCipher.encrypt("segment3")).thenReturn("encrypted_segment3");
        mUpdater = createUpdater(toAdd, toRemove, 10);

        mUpdater.run();

        verify(mCipher).encrypt("segment1");
        verify(mCipher).encrypt("segment2");
        verify(mCipher).encrypt("segment3");
        verify(mCipher).encrypt("segment4");
        verify(mDao).delete(argThat(argument -> argument.size() == 1 &&
                argument.get(0).equals("encrypted_segment3")));
        verify(mDao).insert(argThat((ArgumentMatcher<List<RuleBasedSegmentEntity>>) argument -> argument.size() == 1 &&
                argument.get(0).getName().equals("encrypted_segment1")));
    }

    @Test
    public void rbsInsertEncryptFailureRefusesWrite() {
        Set<RuleBasedSegment> toAdd = Set.of(createRuleBasedSegment("segment1"));
        when(mCipher.encrypt(any())).thenThrow(new IllegalStateException("keystore"));
        mUpdater = createUpdater(toAdd, Collections.emptySet(), 10);

        mUpdater.run();

        verify(mGeneralInfoStorage).setRbsChangeNumber(-1);
        verify(mGeneralInfoStorage, never()).setRbsChangeNumber(10);
        assertTrue(RefusedWrites.ruleBasedSegmentsRefused(mDatabase));
    }

    @Test
    public void rbsNullEncryptedNameRefusesWrite() {
        Set<RuleBasedSegment> toRemove = Set.of(createRuleBasedSegment("segment1"));
        when(mCipher.encrypt("segment1")).thenReturn(null);
        mUpdater = createUpdater(Collections.emptySet(), toRemove, 10);

        mUpdater.run();

        verify(mGeneralInfoStorage).setRbsChangeNumber(-1);
        verify(mGeneralInfoStorage, never()).setRbsChangeNumber(10);
        assertTrue(RefusedWrites.ruleBasedSegmentsRefused(mDatabase));
    }

    @Test
    public void rbsNullNameForInsertRefusesWrite() {
        Set<RuleBasedSegment> toAdd = Set.of(createRuleBasedSegment("segment1"));
        when(mCipher.encrypt(anyString())).thenAnswer(invocation -> "encrypted_" + invocation.getArgument(0));
        when(mCipher.encrypt("segment1")).thenReturn(null);
        mUpdater = createUpdater(toAdd, Collections.emptySet(), 10);

        mUpdater.run();

        verify(mGeneralInfoStorage).setRbsChangeNumber(-1);
        assertTrue(RefusedWrites.ruleBasedSegmentsRefused(mDatabase));
    }

    @Test
    public void rbsNullBodyRefusesWrite() {
        Set<RuleBasedSegment> toAdd = Set.of(createRuleBasedSegment("segment1"));
        when(mCipher.encrypt(anyString())).thenReturn(null);
        when(mCipher.encrypt("segment1")).thenReturn("encrypted_segment1");
        mUpdater = createUpdater(toAdd, Collections.emptySet(), 10);

        mUpdater.run();

        verify(mGeneralInfoStorage).setRbsChangeNumber(-1);
        verify(mGeneralInfoStorage, never()).setRbsChangeNumber(10);
        assertTrue(RefusedWrites.ruleBasedSegmentsRefused(mDatabase));
    }

    @Test
    public void rbsRefusedDatabaseWritesMinusOne() {
        RefusedWrites.refuseRuleBasedSegments(mDatabase);
        mUpdater = createUpdater(Collections.emptySet(), Collections.emptySet(), 10);

        mUpdater.run();

        verify(mGeneralInfoStorage).setRbsChangeNumber(-1);
        verify(mGeneralInfoStorage, never()).setRbsChangeNumber(10);
    }

    @Test
    public void flagsRefusalLeavesRbsChangeNumberAlone() {
        RefusedWrites.refuseFlags(mDatabase);
        mUpdater = createUpdater(Collections.emptySet(), Collections.emptySet(), 10);

        mUpdater.run();

        verify(mGeneralInfoStorage).setRbsChangeNumber(10);
        assertFalse(RefusedWrites.ruleBasedSegmentsRefused(mDatabase));
    }

    @Test
    public void rbsRefusalLeavesFlagsAlone() {
        when(mCipher.encrypt("segment1")).thenReturn(null);
        mUpdater = createUpdater(Collections.emptySet(), Set.of(createRuleBasedSegment("segment1")), 10);

        mUpdater.run();

        assertFalse(RefusedWrites.flagsRefused(mDatabase));
    }

    @NonNull
    private Updater createUpdater(Set<RuleBasedSegment> toAdd, Set<RuleBasedSegment> toRemove, long changeNumber) {
        return new Updater(mCipher, mDao, mGeneralInfoStorage, toAdd, toRemove, changeNumber, mDatabase);
    }
}
