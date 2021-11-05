package io.split.android.client.storage.attributes;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.attributes.AttributesEntity;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class SqLitePersistentAttributesStorage implements PersistentAttributesStorage {

    private static final Type ATTRIBUTES_MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();
    private final SplitRoomDatabase mDatabase;
    private final String mUserKey;

    public SqLitePersistentAttributesStorage(@NonNull SplitRoomDatabase database, @NonNull String userKey) {
        mDatabase = database;
        mUserKey = userKey;
    }

    @Override
    public void set(Map<String, Object> attributes) {
        if (attributes == null) return;

        AttributesEntity entity = new AttributesEntity(mUserKey, Json.toJson(attributes), System.currentTimeMillis() / 1000);

        mDatabase.attributesDao().update(entity);
    }

    @Override
    public Map<String, Object> get() {
        AttributesEntity attributesEntity = mDatabase.attributesDao().getByUserKey(mUserKey);

        return getAttributesMapFromEntity(attributesEntity);
    }

    @Override
    public void clear() {
        mDatabase.attributesDao().deleteAll(mUserKey);
    }

    private Map<String, Object> getAttributesMapFromEntity(AttributesEntity attributesEntity) {
        Map<String, Object> attributesMap = new HashMap<>();

        try {
            attributesMap = Json.fromJson(attributesEntity.getAttributes(), ATTRIBUTES_MAP_TYPE);
        } catch (JsonSyntaxException exception) {
            Logger.e(exception);
        }

        return attributesMap;
    }
}
