package io.split.android.client.storage.mysegments;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.MySegmentDao;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;

public class SqLitePersistentMySegmentsStorageTest {

    @Mock
    private SplitRoomDatabase mockDatabase;
    @Mock
    private SplitCipher mockSplitCipher;
    @Mock
    private MySegmentDao mockMySegmentDao;
    private SqLitePersistentMySegmentsStorage storage;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(mockDatabase.mySegmentDao()).thenReturn(mockMySegmentDao);
        storage = new SqLitePersistentMySegmentsStorage(mockDatabase, mockSplitCipher);
    }

    @Test
    public void testSet() {
        String userKey = "user_key";
        List<String> segments = Arrays.asList("segment1", "segment2", "segment3");
        String encryptedSegments = "encrypted_segments";

        when(mockSplitCipher.encrypt(anyString())).thenReturn(encryptedSegments);

        storage.set(userKey, segments);

        verify(mockSplitCipher).encrypt("segment1,segment2,segment3");
        verify(mockMySegmentDao).update(any(MySegmentEntity.class));
    }

    @Test
    public void testGetSnapshot() {
        String userKey = "user_key";
        String encryptedSegments = "encrypted_segments";
        String decryptedSegments = "segment1,segment2,segment3";
        MySegmentEntity entity = new MySegmentEntity();
        entity.setUserKey(userKey);
        entity.setSegmentList(encryptedSegments);

        when(mockMySegmentDao.getByUserKey(userKey)).thenReturn(entity);
        when(mockSplitCipher.decrypt(encryptedSegments)).thenReturn(decryptedSegments);

        List<String> result = storage.getSnapshot(userKey);

        assertEquals(Arrays.asList("segment1", "segment2", "segment3"), result);
    }

    @Test
    public void testGetSnapshotWithEmptySegments() {
        String userKey = "user_key";
        MySegmentEntity entity = new MySegmentEntity();
        entity.setUserKey(userKey);
        entity.setSegmentList(null);

        when(mockMySegmentDao.getByUserKey(userKey)).thenReturn(entity);

        List<String> result = storage.getSnapshot(userKey);

        assertEquals(0, result.size());
    }
}
