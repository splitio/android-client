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
    private SplitRoomDatabase mDatabase;
    @Mock
    private SplitCipher mSplitCipher;
    @Mock
    private MySegmentDao mDao;
    private SqLitePersistentMySegmentsStorage mStorage;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(mDatabase.mySegmentDao()).thenReturn(mDao);
        mStorage = new SqLitePersistentMySegmentsStorage(mDatabase, mSplitCipher);
    }

    @Test
    public void encryptedValuesAreStoredWithDao() {
        String userKey = "user_key";
        List<String> segments = Arrays.asList("segment1", "segment2", "segment3");
        String encryptedSegments = "encrypted_segments";

        when(mSplitCipher.encrypt(anyString())).thenReturn(encryptedSegments);

        mStorage.set(userKey, segments);

        verify(mSplitCipher).encrypt("segment1,segment2,segment3");
        verify(mDao).update(any(MySegmentEntity.class));
    }

    @Test
    public void getSnapshotReturnsDecryptedValues() {
        String userKey = "user_key";
        String encryptedSegments = "encrypted_segments";
        String decryptedSegments = "segment1,segment2,segment3";
        MySegmentEntity entity = new MySegmentEntity();
        entity.setUserKey(userKey);
        entity.setSegmentList(encryptedSegments);

        when(mDao.getByUserKey(userKey)).thenReturn(entity);
        when(mSplitCipher.decrypt(encryptedSegments)).thenReturn(decryptedSegments);

        List<String> result = mStorage.getSnapshot(userKey);

        assertEquals(Arrays.asList("segment1", "segment2", "segment3"), result);
    }
}
