package io.split.android.client.service.synchronizer.attributes;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.attributes.AttributeTaskFactory;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public class AttributesSynchronizerFactoryImpl implements AttributesSynchronizerFactory {

    private final SplitTaskExecutor mTaskExecutor;
    private final PersistentAttributesStorage mPersistentAttributeStorage;

    public AttributesSynchronizerFactoryImpl(@NonNull SplitTaskExecutor taskExecutor,
                                             @NonNull PersistentAttributesStorage persistentAttributesStorage) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mPersistentAttributeStorage = checkNotNull(persistentAttributesStorage);
    }

    @Override
    public AttributesSynchronizer getSynchronizer(AttributeTaskFactory attributeTaskFactory, SplitEventsManager splitEventsManager) {
        return new AttributesSynchronizerImpl(mTaskExecutor, splitEventsManager, attributeTaskFactory, mPersistentAttributeStorage);
    }
}
