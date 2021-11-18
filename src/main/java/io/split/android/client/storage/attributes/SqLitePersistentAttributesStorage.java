package io.split.android.client.storage.attributes;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.split.android.client.storage.db.attributes.AttributesDao;
import io.split.android.client.storage.db.attributes.AttributesEntity;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class SqLitePersistentAttributesStorage implements PersistentAttributesStorage {

    private static final Type ATTRIBUTES_MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();
    private final AttributesDao mAttributesDao;
    private final String mUserKey;

    public SqLitePersistentAttributesStorage(@NonNull AttributesDao attributesDao, @NonNull String userKey) {
        mAttributesDao = checkNotNull(attributesDao);
        mUserKey = userKey;
    }

    @Override
    public void set(@Nullable Map<String, Object> attributes) {
        if (attributes == null) return;

        AttributesEntity entity = new AttributesEntity(mUserKey, Json.toJson(attributes), System.currentTimeMillis() / 1000);

        mAttributesDao.update(entity);
    }

    @NonNull
    @Override
    public Map<String, Object> getAll() {
        AttributesEntity attributesEntity = mAttributesDao.getByUserKey(mUserKey);

        return getAttributesMapFromEntity(attributesEntity);
    }

    @Override
    public void clear() {
        mAttributesDao.deleteAll(mUserKey);
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
