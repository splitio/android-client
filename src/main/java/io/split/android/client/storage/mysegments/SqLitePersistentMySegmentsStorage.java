package io.split.android.client.storage.mysegments;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.StringHelper;

public class SqLitePersistentMySegmentsStorage implements PersistentMySegmentsStorage {

    final SplitRoomDatabase mDatabase;
    final StringHelper mStringHelper;

    public SqLitePersistentMySegmentsStorage(@NonNull SplitRoomDatabase database) {
        mDatabase = checkNotNull(database);
        mStringHelper = new StringHelper();
    }

    @Override
    public void set(String userKey, @NonNull List<String> mySegments) {
        if (mySegments == null) {
            return;
        }
        MySegmentEntity entity = new MySegmentEntity();
        entity.setUserKey(userKey);
        entity.setSegmentList(mStringHelper.join(",", mySegments));
        entity.setUpdatedAt(System.currentTimeMillis() / 1000);
        mDatabase.mySegmentDao().update(entity);
    }

    @Override
    public List<String> getSnapshot(String userKey) {
        return getMySegmentsFromEntity(mDatabase.mySegmentDao().getByUserKey(userKey));
    }

    @Override
    public void close() {
    }

    private static List<String> getMySegmentsFromEntity(MySegmentEntity entity) {
        if (entity == null || Strings.isNullOrEmpty(entity.getSegmentList())) {
            return new ArrayList<>();
        }
        return Arrays.asList(entity.getSegmentList().split(","));
    }
}
