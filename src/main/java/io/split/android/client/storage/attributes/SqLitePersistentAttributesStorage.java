package io.split.android.client.storage.attributes;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import io.split.android.client.storage.db.attributes.AttributesDao;
import io.split.android.client.storage.db.attributes.AttributesEntity;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class SqLitePersistentAttributesStorage implements PersistentAttributesStorage {

    private static final Type ATTRIBUTES_MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();
    private final AttributesDao mAttributesDao;

    public SqLitePersistentAttributesStorage(@NonNull AttributesDao attributesDao, @NonNull String userKey) {
        mAttributesDao = checkNotNull(attributesDao);
    }

    @Override
    public void set(String matchingKey, @Nullable Map<String, Object> attributes) {
        if (attributes == null) {
            return;
        }

        AttributesEntity entity = new AttributesEntity(matchingKey, Json.toJson(attributes), System.currentTimeMillis() / 1000);

        mAttributesDao.update(entity);
    }

    @NonNull
    @Override
    public Map<String, Object> getAll(String matchingKey) {
        AttributesEntity attributesEntity = mAttributesDao.getByUserKey(matchingKey);

        return getAttributesMapFromEntity(attributesEntity);
    }

    @Override
    public void clear(String matchingKey) {
        mAttributesDao.deleteAll(matchingKey);
    }

    private Map<String, Object> getAttributesMapFromEntity(AttributesEntity attributesEntity) {
        Map<String, Object> attributesMap = new HashMap<>();

        if (attributesEntity != null) {
            try {
                attributesMap = Json.genericValueMapFromJson(attributesEntity.getAttributes(), ATTRIBUTES_MAP_TYPE);
            } catch (JsonSyntaxException exception) {
                Logger.e(exception);
            }
        }

        return attributesMap;
    }
}
