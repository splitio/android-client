package io.split.android.client.service.synchronizer.attributes;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.attributes.AttributeTaskFactory;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.synchronizer.LoadLocalDataListener;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public class AttributesSynchronizerImpl implements AttributesSynchronizer {

    private final SplitTaskExecutor mTaskExecutor;
    private final AttributeTaskFactory mAttributeTaskFactory;
    private final LoadLocalDataListener mLoadLocalAttributesListener;
    private final PersistentAttributesStorage mPersistentAttributeStorage;

    AttributesSynchronizerImpl(SplitTaskExecutor taskExecutor,
                               SplitEventsManager splitEventsManager,
                               AttributeTaskFactory attributeTaskFactory,
                               PersistentAttributesStorage persistentAttributesStorage) {
        mTaskExecutor = taskExecutor;
        mAttributeTaskFactory = attributeTaskFactory;
        mPersistentAttributeStorage = persistentAttributesStorage;
        mLoadLocalAttributesListener = new LoadLocalDataListener(
                splitEventsManager, SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE);
    }

    @Override
    public void loadAttributesFromCache() {
        mTaskExecutor.submit(mAttributeTaskFactory.createLoadAttributesTask(mPersistentAttributeStorage),
                mLoadLocalAttributesListener);
    }
}
