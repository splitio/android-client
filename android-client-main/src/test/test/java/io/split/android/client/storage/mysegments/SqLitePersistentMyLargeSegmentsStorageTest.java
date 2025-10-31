package io.split.android.client.storage.mysegments;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.split.android.client.dtos.SegmentsChange;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.MyLargeSegmentDao;
import io.split.android.client.storage.db.MyLargeSegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;

public class SqLitePersistentMyLargeSegmentsStorageTest {

    @Mock
    private SplitRoomDatabase mDatabase;
    @Mock
    private SplitCipher mSplitCipher;
    @Mock
    private MyLargeSegmentDao mDao;
    private SqLitePersistentMySegmentsStorage<MyLargeSegmentEntity> mStorage;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(mDatabase.myLargeSegmentDao()).thenReturn(mDao);
        mStorage = new SqLitePersistentMySegmentsStorage<>(mSplitCipher, mDatabase.myLargeSegmentDao(), MyLargeSegmentEntity.creator());
    }

    @Test
    public void encryptedValuesAreStoredWithDao() {
        String userKey = "user_key";
        Set<String> segments = new HashSet<>(Arrays.asList("segment1", "segment2", "segment3"));
        String encryptedSegments = "encrypted_segments";

        when(mSplitCipher.encrypt(anyString())).thenReturn(encryptedSegments);

        mStorage.set(userKey, SegmentsChange.create(segments, 2415L));

        verify(mSplitCipher).encrypt(argThat(new ArgumentMatcher<String>() {
            @Override
            public boolean matches(String argument) {
                if (argument.contains("{")) {
                    SegmentsChange segmentsChange = Json.fromJson(argument, SegmentsChange.class);
                    return segmentsChange.getSegments().size() == 3 &&
                            segmentsChange.getNames().containsAll(Arrays.asList("segment1", "segment2", "segment3")) &&
                            segmentsChange.getChangeNumber() == 2415;
                } else {
                    return false;
                }
            }
        }));
        verify(mDao).update(any(MyLargeSegmentEntity.class));
    }

    @Test
    public void noUpdatesAreMadeWhenEncryptionResultIsNull() {
        String userKey = "user_key";
        Set<String> segments = new HashSet<>(Arrays.asList("segment1", "segment2", "segment3"));

        when(mSplitCipher.encrypt(anyString())).thenReturn(null);

        mStorage.set(userKey, SegmentsChange.create(segments, 2415L));

        verify(mSplitCipher).encrypt(argThat(new ArgumentMatcher<String>() {
            @Override
            public boolean matches(String argument) {
                if (argument.contains("{")) {
                    SegmentsChange segmentsChange = Json.fromJson(argument, SegmentsChange.class);
                    return segmentsChange.getSegments().size() == 3 &&
                            segmentsChange.getNames().containsAll(Arrays.asList("segment1", "segment2", "segment3")) &&
                            segmentsChange.getChangeNumber() == 2415;
                } else {
                    return false;
                }
            }
        }));
        verifyNoInteractions(mDao);
    }

    @Test
    public void getSnapshotReturnsDecryptedValues() {
        String userKey = "user_key";
        String encryptedSegments = "encrypted_segments";
        String decryptedSegments = "segment1,segment2,segment3";
        MyLargeSegmentEntity entity = new MyLargeSegmentEntity();
        entity.setUserKey(userKey);
        entity.setSegmentList(encryptedSegments);

        when(mDao.getByUserKey("encrypted_user_key")).thenReturn(entity);
        when(mSplitCipher.encrypt(userKey)).thenReturn("encrypted_user_key");
        when(mSplitCipher.decrypt(encryptedSegments)).thenReturn(decryptedSegments);

        SegmentsChange result = mStorage.getSnapshot(userKey);

        assertTrue(result.getNames().containsAll(Arrays.asList("segment1", "segment2", "segment3")));
    }
}
