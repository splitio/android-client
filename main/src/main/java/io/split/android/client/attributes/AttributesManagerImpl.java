package io.split.android.client.attributes;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import io.split.android.client.service.attributes.AttributeTaskFactory;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;
import io.split.android.client.validators.AttributesValidator;
import io.split.android.client.validators.ValidationMessageLogger;

public class AttributesManagerImpl implements AttributesManager {

    private static final long PERSIST_TASK_DELAY_SECONDS = 5L;

    private final AttributesStorage mAttributesStorage;
    private final AttributesValidator mAttributesValidator;
    private final ValidationMessageLogger mValidationMessageLogger;
    @Nullable
    private final PersistentAttributesStorage mPersistentAttributesStorage;
    @Nullable
    private final AttributeTaskFactory mAttributeTaskFactory;
    @Nullable
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final Object mScheduledPersistTaskLock = new Object();
    @Nullable
    private String mScheduledPersistTaskId;
    @Nullable
    private ScheduledTaskKind mScheduledPersistTaskKind;

    private enum ScheduledTaskKind {
        UPDATE, CLEAR
    }

    AttributesManagerImpl(@NonNull AttributesStorage attributesStorage,
                          @NonNull AttributesValidator attributesValidator,
                          @NonNull ValidationMessageLogger validationMessageLogger,
                          @Nullable PersistentAttributesStorage persistentAttributesStorage,
                          @Nullable AttributeTaskFactory attributeTaskFactory,
                          @Nullable SplitTaskExecutor splitTaskExecutor) {
        mAttributesStorage = checkNotNull(attributesStorage);
        mAttributesValidator = checkNotNull(attributesValidator);
        mValidationMessageLogger = checkNotNull(validationMessageLogger);
        mPersistentAttributesStorage = persistentAttributesStorage;
        mAttributeTaskFactory = attributeTaskFactory;
        mSplitTaskExecutor = splitTaskExecutor;
    }

    @Override
    public boolean setAttribute(String attributeName, Object value) {
        if (!mAttributesValidator.isValid(value)) {
            logValidationWarning(attributeName);
            return false;
        }

        mAttributesStorage.set(attributeName, value);

        submitUpdateTask(mPersistentAttributesStorage);

        return true;
    }

    @Nullable
    @Override
    public Object getAttribute(String attributeName) {
        return mAttributesStorage.get(attributeName);
    }

    @Override
    public boolean setAttributes(Map<String, Object> attributes) {
        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
            if (!mAttributesValidator.isValid(attribute.getValue())) {
                logValidationWarning(attribute.getKey());
                return false;
            }
        }

        mAttributesStorage.set(attributes);

        submitUpdateTask(mPersistentAttributesStorage);

        return true;
    }

    private void logValidationWarning(String key) {
        String mValidationTag = "split attributes";
        mValidationMessageLogger.w("You passed an invalid attribute value for " + key + ", acceptable types are String, double, float, long, int, boolean or Collections", mValidationTag);
    }

    @NonNull
    @Override
    public Map<String, Object> getAllAttributes() {
        return mAttributesStorage.getAll();
    }

    @Override
    public boolean removeAttribute(String attributeName) {
        mAttributesStorage.remove(attributeName);

        submitUpdateTask(mPersistentAttributesStorage);

        return true;
    }

    @Override
    public boolean clearAttributes() {
        mAttributesStorage.clear();

        submitClearTask(mPersistentAttributesStorage);

        return true;
    }

    private void submitUpdateTask(@Nullable PersistentAttributesStorage persistentStorage) {
        schedulePersistTask(persistentStorage, ScheduledTaskKind.UPDATE);
    }

    private void submitClearTask(@Nullable PersistentAttributesStorage persistentStorage) {
        schedulePersistTask(persistentStorage, ScheduledTaskKind.CLEAR);
    }

    /**
     * Schedules a single pending persistence task of the given kind, coalescing repeated calls:
     * while a task of the same kind is already pending, no new one is scheduled. A pending task of
     * a different kind is cancelled and replaced, so that a clear is never overwritten by a stale
     * update (or vice versa).
     */
    private void schedulePersistTask(@Nullable PersistentAttributesStorage persistentStorage, ScheduledTaskKind kind) {
        if (persistentStorage == null || mSplitTaskExecutor == null || mAttributeTaskFactory == null) {
            return;
        }

        synchronized (mScheduledPersistTaskLock) {
            if (mScheduledPersistTaskId != null && mScheduledPersistTaskKind != kind) {
                mSplitTaskExecutor.stopTask(mScheduledPersistTaskId);
                clearScheduledSlot();
            }

            if (mScheduledPersistTaskId != null) {
                return;
            }

            SplitTask task = kind == ScheduledTaskKind.CLEAR ?
                    mAttributeTaskFactory.createAttributeClearTask(persistentStorage) :
                    mAttributeTaskFactory.createAttributeUpdateTask(persistentStorage);

            // The wrapped task needs the task id, which is only known after scheduling, hence the holder.
            // The slot is cleared at the START of execution (not on completion) so that a mutation
            // landing while the task's execute() is still running (e.g. a slow DB write) is not
            // silently deduped.
            String[] idHolder = new String[1];
            SplitTask wrapped = () -> {
                clearScheduledSlotIfCurrent(idHolder[0]);
                return task.execute();
            };
            String taskId = mSplitTaskExecutor.schedule(wrapped, PERSIST_TASK_DELAY_SECONDS, null);
            idHolder[0] = taskId;

            mScheduledPersistTaskId = taskId;
            mScheduledPersistTaskKind = kind;
        }
    }

    private void clearScheduledSlotIfCurrent(@Nullable String taskId) {
        synchronized (mScheduledPersistTaskLock) {
            if (taskId != null && taskId.equals(mScheduledPersistTaskId)) {
                clearScheduledSlot();
            }
        }
    }

    private void clearScheduledSlot() {
        mScheduledPersistTaskId = null;
        mScheduledPersistTaskKind = null;
    }
}
