package io.split.android.client.storage.mysegments;

import androidx.annotation.NonNull;
import androidx.room.util.StringUtil;

import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import io.split.android.client.dtos.Split;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.StringHelper;

import static com.google.common.base.Preconditions.checkNotNull;

public class SqLitePersistentMySegmentsStorage implements PersistentMySegmentsStorage {

    private static final Type MY_SEGMENTS_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();
    final SplitRoomDatabase mDatabase;
    final String mUserKey;
    final StringHelper mStringHelper;

    public SqLitePersistentMySegmentsStorage(@NonNull SplitRoomDatabase database, @NonNull String userKey) {
        checkNotNull(database);
        checkNotNull(userKey);

        mDatabase = database;
        mUserKey = userKey;
        mStringHelper = new StringHelper();
    }

    @Override
    public void set(List<String> mySegments) {
        MySegmentEntity entity = new MySegmentEntity();
        entity.setUserKey(mUserKey);
        entity.setSegmentList(mStringHelper.join(",", mySegments));
        entity.setUpdatedAt(System.currentTimeMillis() / 1000);
        mDatabase.mySegmentDao().update(entity);
    }

    @Override
    public List<String> getSnapshot() {
        return getMySegmentsFromEntity(mDatabase.mySegmentDao().getByUserKeys(mUserKey));
    }

    @Override
    public void close() {
    }


    private List<String> getMySegmentsFromEntity(MySegmentEntity entity) {
        List<String> mySegments = new ArrayList<>();
        if (entity == null) {
            return mySegments;
        }

        try {
            mySegments.add(Json.fromJson(entity.getSegmentList(), MY_SEGMENTS_LIST_TYPE));
        } catch (JsonSyntaxException e) {
            Logger.e("Could not parse my segments entity for key: " + entity.getUserKey());
        }
        return mySegments;
    }
}