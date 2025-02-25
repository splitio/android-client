package io.split.android.client.storage.rbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentDao;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentEntity;
import io.split.android.client.storage.general.GeneralInfoStorage;

public class SnapshotLoaderTest {

    private RuleBasedSegmentDao mDao;
    private SplitCipher mCipher;
    private GeneralInfoStorage mGeneralInfoStorage;
    private SnapshotLoader mSnapshotLoader;

    @Before
    public void setUp() {
        mDao = mock(RuleBasedSegmentDao.class);
        mCipher = mock(SplitCipher.class);
        mGeneralInfoStorage = mock(GeneralInfoStorage.class);
        mSnapshotLoader = new SnapshotLoader(mDao, mCipher, mGeneralInfoStorage);
    }

    @Test
    public void callReturnsCorrectSnapshotWithDecryptedSegments() throws Exception {
        long expectedChangeNumber = 123L;
        when(mGeneralInfoStorage.getFlagsChangeNumber()).thenReturn(expectedChangeNumber);

        RuleBasedSegmentEntity entity1 = new RuleBasedSegmentEntity("segment1", "encryptedBody1", System.currentTimeMillis());
        RuleBasedSegmentEntity entity2 = new RuleBasedSegmentEntity("segment2", "encryptedBody2", System.currentTimeMillis());
        List<RuleBasedSegmentEntity> entities = Arrays.asList(entity1, entity2);
        when(mDao.getAll()).thenReturn(entities);

        when(mCipher.decrypt("segment1")).thenReturn("segment1");
        when(mCipher.decrypt("segment2")).thenReturn("segment2");
        when(mCipher.decrypt("encryptedBody1")).thenAnswer(invocation -> "{ \"name\": \"segment1\", \"trafficTypeName\": \"user\", \"changeNumber\": 1 }");
        when(mCipher.decrypt("encryptedBody2")).thenAnswer(invocation -> "{ \"name\": \"segment2\", \"trafficTypeName\": \"user\", \"changeNumber\": 2 }");

        RuleBasedSegmentSnapshot result = mSnapshotLoader.call();

        assertNotNull(result);
        assertEquals(expectedChangeNumber, result.getChangeNumber());

        Map<String, RuleBasedSegment> segments = result.getSegments();
        assertEquals(2, segments.size());
        RuleBasedSegment rbs1 = segments.get("segment1");
        assertNotNull(rbs1);
        assertEquals("segment1", rbs1.getName());
        assertEquals("user", rbs1.getTrafficTypeName());
        assertEquals(1, rbs1.getChangeNumber());

        RuleBasedSegment rbs2 = segments.get("segment2");
        assertNotNull(rbs2);
        assertEquals("segment2", rbs2.getName());
        assertEquals("user", rbs2.getTrafficTypeName());
        assertEquals(2, rbs2.getChangeNumber());
    }

    @Test
    public void callGetsChangeNumberFromGeneralInfoStorage() {
        mSnapshotLoader.call();

        verify(mGeneralInfoStorage).getFlagsChangeNumber();
    }

    @Test
    public void callGetsAllSegmentsFromDao() {
        mSnapshotLoader.call();

        verify(mDao).getAll();
    }

    @Test
    public void callDecryptsNameAndBodyFromEntity() {
        when(mDao.getAll()).thenReturn(Arrays.asList(
                new RuleBasedSegmentEntity("segment1", "encryptedBody1", System.currentTimeMillis()),
                new RuleBasedSegmentEntity("segment2", "encryptedBody2", System.currentTimeMillis())));

        mSnapshotLoader.call();

        verify(mCipher).decrypt("segment1");
        verify(mCipher).decrypt("segment2");
        verify(mCipher).decrypt("encryptedBody1");
        verify(mCipher).decrypt("encryptedBody2");
    }

    @Test
    public void constructorThrowsNullPointerExceptionWhenDaoIsNull() {
        assertThrows(NullPointerException.class,
                () -> new SnapshotLoader(null, mCipher, mGeneralInfoStorage));
    }

    @Test
    public void constructorThrowsNullPointerExceptionWhenCipherIsNull() {
        assertThrows(NullPointerException.class,
                () -> new SnapshotLoader(mDao, null, mGeneralInfoStorage));
    }

    @Test
    public void constructorThrowsNullPointerExceptionWhenGeneralInfoStorageIsNull() {
        assertThrows(NullPointerException.class,
                () -> new SnapshotLoader(mDao, mCipher, null));
    }
}