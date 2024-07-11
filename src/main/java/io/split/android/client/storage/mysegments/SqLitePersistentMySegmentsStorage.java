package io.split.android.client.storage.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.SegmentDao;
import io.split.android.client.storage.db.SegmentEntity;
import io.split.android.client.utils.StringHelper;
import io.split.android.client.utils.Utils;
import io.split.android.client.utils.logger.Logger;

public class SqLitePersistentMySegmentsStorage<T extends SegmentEntity> implements PersistentMySegmentsStorage {

    private final SegmentDao<T> mDao;
    private final StringHelper mStringHelper;
    private final SplitCipher mSplitCipher;
    private final SegmentEntity.Creator<T> mCreator;

    public SqLitePersistentMySegmentsStorage(@NonNull SplitCipher splitCipher, SegmentDao<T> mySegmentDao, SegmentEntity.Creator<T> creator) {
        mDao = mySegmentDao;
        mStringHelper = new StringHelper();
        mSplitCipher = checkNotNull(splitCipher);
        mCreator = checkNotNull(creator);
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
        T entity = mCreator.createEntity(encryptedUserKey, encryptedSegmentList, System.currentTimeMillis() / 1000);
        mDao.update(entity);
    }

    @Override
    public List<String> getSnapshot(String userKey) {
        String encryptedUserKey = mSplitCipher.encrypt(userKey);
        return getMySegmentsFromEntity(mDao.getByUserKey(encryptedUserKey));
    }

    @Override
    public void close() {
    }

    private List<String> getMySegmentsFromEntity(SegmentEntity entity) {
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
