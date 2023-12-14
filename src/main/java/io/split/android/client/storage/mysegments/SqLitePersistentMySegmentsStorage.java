package io.split.android.client.storage.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.StringHelper;
import io.split.android.client.utils.Utils;
import io.split.android.client.utils.logger.Logger;

public class SqLitePersistentMySegmentsStorage implements PersistentMySegmentsStorage {

    private final SplitRoomDatabase mDatabase;
    private final StringHelper mStringHelper;
    private final SplitCipher mSplitCipher;

    public SqLitePersistentMySegmentsStorage(@NonNull SplitRoomDatabase database,
                                             @NonNull SplitCipher splitCipher) {
        mDatabase = checkNotNull(database);
        mStringHelper = new StringHelper();
        mSplitCipher = checkNotNull(splitCipher);
    }

    @Override
    public void set(String userKey, @NonNull List<String> mySegments) {
        if (mySegments == null) {
            return;
        }

        String encryptedUserKey = mSplitCipher.encrypt(userKey);
        String encryptedSegmentList = mSplitCipher.encrypt(mStringHelper.join(",", mySegments));
        if (encryptedUserKey == null || encryptedSegmentList == null) {
            Logger.e("Error encrypting my segments");
            return;
        }
        MySegmentEntity entity = new MySegmentEntity();
        entity.setUserKey(encryptedUserKey);
        entity.setSegmentList(encryptedSegmentList);
        entity.setUpdatedAt(System.currentTimeMillis() / 1000);
        mDatabase.mySegmentDao().update(entity);
    }

    @Override
    public List<String> getSnapshot(String userKey) {
        String encryptedUserKey = mSplitCipher.encrypt(userKey);
        return getMySegmentsFromEntity(mDatabase.mySegmentDao().getByUserKey(encryptedUserKey));
    }

    @Override
    public void close() {
    }

    private List<String> getMySegmentsFromEntity(MySegmentEntity entity) {
        if (entity == null || Utils.isNullOrEmpty(entity.getSegmentList())) {
            return new ArrayList<>();
        }

        String segmentList = mSplitCipher.decrypt(entity.getSegmentList());
        if (segmentList == null) {
            return new ArrayList<>();
        }
        String[] segments = segmentList.split(",");
        return Arrays.asList(segments);
    }
}
