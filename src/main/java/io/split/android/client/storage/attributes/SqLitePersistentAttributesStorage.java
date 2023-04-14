package io.split.android.client.storage.attributes;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.attributes.AttributesDao;
import io.split.android.client.storage.db.attributes.AttributesEntity;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class SqLitePersistentAttributesStorage implements PersistentAttributesStorage {

    private static final Type ATTRIBUTES_MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();
    private final AttributesDao mAttributesDao;
    private final SplitCipher mSplitCipher;

    public SqLitePersistentAttributesStorage(@NonNull AttributesDao attributesDao,
                                             @NonNull SplitCipher splitCipher) {
        mAttributesDao = checkNotNull(attributesDao);
        mSplitCipher = checkNotNull(splitCipher);
    }

    @Override
    public void set(String matchingKey, @Nullable Map<String, Object> attributes) {
        if (attributes == null) {
            return;
        }

        AttributesEntity entity = new AttributesEntity(matchingKey,
                mSplitCipher.encrypt(Json.toJson(attributes)), System.currentTimeMillis() / 1000);

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
                attributesMap = Json.genericValueMapFromJson(mSplitCipher.decrypt(attributesEntity.getAttributes()),
                        ATTRIBUTES_MAP_TYPE);
            } catch (JsonSyntaxException exception) {
                Logger.e(exception);
            }
        }

        return attributesMap;
    }
}
