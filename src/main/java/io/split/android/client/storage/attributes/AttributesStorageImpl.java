package io.split.android.client.storage.attributes;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.service.attributes.AttributeTaskFactory;
import io.split.android.client.service.executor.SplitTaskExecutor;

public class AttributesStorageImpl implements AttributesStorage {

    @Nullable
    private final PersistentAttributesStorage mPersistentAttributesStorage;
    @Nullable
    private final SplitTaskExecutor mSplitTaskExecutor;
    @Nullable
    private final AttributeTaskFactory mAttributeTaskFactory;
    private final Map<String, Object> mInMemoryAttributes = new ConcurrentHashMap<>();

    public AttributesStorageImpl() {
        mPersistentAttributesStorage = null;
        mSplitTaskExecutor = null;
        mAttributeTaskFactory = null;
    }

    public AttributesStorageImpl(@NonNull PersistentAttributesStorage persistentAttributesStorage,
                                 @NonNull SplitTaskExecutor splitTaskExecutor,
                                 @NonNull AttributeTaskFactory attributeTaskFactory) {
        mPersistentAttributesStorage = checkNotNull(persistentAttributesStorage);
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mAttributeTaskFactory = checkNotNull(attributeTaskFactory);
    }

    @Override
    public void loadLocal() {
        if (mPersistentAttributesStorage != null) {
            mInMemoryAttributes.clear();
            mInMemoryAttributes.putAll(mPersistentAttributesStorage.getAll());
        }
    }

    @Nullable
    @Override
    public Object get(String name) {
        return mInMemoryAttributes.get(name);
    }

    @NonNull
    @Override
    public Map<String, Object> getAll() {
        return new HashMap<>(mInMemoryAttributes);
    }

    @Override
    public void set(String name, @NonNull Object value) {
        mInMemoryAttributes.put(name, value);

        if (mPersistentAttributesStorage != null) {
            submitUpdateTask(mPersistentAttributesStorage);
        }
    }

    @Override
    public void set(@Nullable Map<String, Object> attributes) {
        if (attributes == null) return;

        mInMemoryAttributes.putAll(attributes);

        if (mPersistentAttributesStorage != null) {
            submitUpdateTask(mPersistentAttributesStorage);
        }
    }

    @Override
    public void clear() {
        mInMemoryAttributes.clear();

        if (mPersistentAttributesStorage != null) {
            submitClearTask(mPersistentAttributesStorage);
        }
    }

    @Override
    public void destroy() {
        mInMemoryAttributes.clear();
    }

    @Override
    public void remove(String key) {
        mInMemoryAttributes.remove(key);

        if (mPersistentAttributesStorage != null) {
            submitUpdateTask(mPersistentAttributesStorage);
        }
    }

    private void submitUpdateTask(PersistentAttributesStorage persistentStorage) {
        if (mSplitTaskExecutor != null && mAttributeTaskFactory != null) {
            mSplitTaskExecutor.submit(mAttributeTaskFactory.createAttributeUpdateTask(persistentStorage, mInMemoryAttributes), null);
        }
    }

    private void submitClearTask(PersistentAttributesStorage persistentStorage) {
        if (mSplitTaskExecutor != null && mAttributeTaskFactory != null) {
            mSplitTaskExecutor.submit(mAttributeTaskFactory.createAttributeClearTask(persistentStorage), null);
        }
    }
}
