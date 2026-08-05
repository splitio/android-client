package io.split.android.client.service.attributes;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;
import io.split.android.client.utils.logger.Logger;

public class UpdateAttributesInPersistentStorageTask implements SplitTask {

    private final String mMatchingKey;
    private final PersistentAttributesStorage mPersistentAttributesStorage;
    private final AttributesStorage mAttributesStorage;

    public UpdateAttributesInPersistentStorageTask(@NonNull String matchingKey, @NonNull PersistentAttributesStorage persistentAttributesStorage, @NonNull AttributesStorage attributesStorage) {
        mMatchingKey = checkNotNull(matchingKey);
        mPersistentAttributesStorage = checkNotNull(persistentAttributesStorage);
        mAttributesStorage = checkNotNull(attributesStorage);
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            mPersistentAttributesStorage.set(mMatchingKey, mAttributesStorage.getAll());
        } catch (Exception e) {
            Logger.e("Error persisting attributes: " + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK);
        }

        return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
    }
}
