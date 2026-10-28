package io.split.android.client.storage.rbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.storage.cipher.CBCCipher;
import io.split.android.client.storage.cipher.JceCbcCipherProvider;
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
        when(mGeneralInfoStorage.getRbsChangeNumber()).thenReturn(expectedChangeNumber);

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
    public void cbcCipherUsesOneOperationPerBatch() {
        JceCbcCipherProvider provider = new JceCbcCipherProvider();
        CBCCipher cbcCipher = new CBCCipher(provider);
        List<RuleBasedSegmentEntity> entities = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String name = "segment" + i;
            entities.add(new RuleBasedSegmentEntity(cbcCipher.encrypt(name), cbcCipher.encrypt(segmentJson(name, i)), 1L));
        }
        when(mDao.getAll()).thenReturn(entities);
        provider.decryptionCalls.set(0);

        RuleBasedSegmentSnapshot snapshot = new SnapshotLoader(mDao, cbcCipher, mGeneralInfoStorage).call();

        assertEquals(1, provider.decryptionCalls.get());
        assertEquals(20, snapshot.getSegments().size());
        for (int i = 0; i < 20; i++) {
            RuleBasedSegment segment = snapshot.getSegments().get("segment" + i);
            assertNotNull(segment);
            assertEquals(i, segment.getChangeNumber());
        }
    }

    @Test
    public void segmentsWithUndecryptableNameOrBodyOrInvalidJsonAreSkippedAndTheRestStayPaired() {
        JceCbcCipherProvider provider = new JceCbcCipherProvider();
        CBCCipher cipher = new CBCCipher(provider);
        byte[] corrupt = new byte[16];
        Arrays.fill(corrupt, (byte) 'z');
        corrupt[15] = 5;
        String undecryptable = java.util.Base64.getEncoder().encodeToString(provider.encryptWithoutPadding(corrupt));
        when(mDao.getAll()).thenReturn(Arrays.asList(
                new RuleBasedSegmentEntity(cipher.encrypt("one"), cipher.encrypt(segmentJson("one", 1)), 1L),
                new RuleBasedSegmentEntity(undecryptable, cipher.encrypt(segmentJson("lost", 2)), 1L),
                new RuleBasedSegmentEntity(cipher.encrypt("three"), cipher.encrypt(segmentJson("three", 3)), 1L),
                new RuleBasedSegmentEntity(cipher.encrypt("four"), undecryptable, 1L),
                new RuleBasedSegmentEntity(cipher.encrypt("five"), cipher.encrypt("{ not json"), 1L),
                new RuleBasedSegmentEntity(cipher.encrypt("six"), cipher.encrypt(segmentJson("six", 6)), 1L)));

        Map<String, RuleBasedSegment> segments = new SnapshotLoader(mDao, cipher, mGeneralInfoStorage).call().getSegments();

        assertEquals(3, segments.size());
        assertEquals(1, segments.get("one").getChangeNumber());
        assertEquals(3, segments.get("three").getChangeNumber());
        assertEquals(6, segments.get("six").getChangeNumber());
    }

    private static String segmentJson(String name, int changeNumber) {
        return "{ \"name\": \"" + name + "\", \"trafficTypeName\": \"user\", \"changeNumber\": " + changeNumber + " }";
    }

    @Test
    public void callGetsChangeNumberFromGeneralInfoStorage() {
        mSnapshotLoader.call();

        verify(mGeneralInfoStorage).getRbsChangeNumber();
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
