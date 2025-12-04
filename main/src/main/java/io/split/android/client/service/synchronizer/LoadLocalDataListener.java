package io.split.android.client.service.synchronizer;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.split.android.client.api.EventMetadata;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;

public class LoadLocalDataListener implements SplitTaskExecutionListener {

    /**
     * Functional interface for providing metadata when the event is fired.
     */
    public interface MetadataProvider {
        @Nullable
        EventMetadata getMetadata();
    }

    private final ISplitEventsManager mSplitEventsManager;
    private final SplitInternalEvent mEventToFire;
    @Nullable
    private final MetadataProvider mMetadataProvider;

    public LoadLocalDataListener(ISplitEventsManager splitEventsManager,
                                 SplitInternalEvent eventToFire) {
        this(splitEventsManager, eventToFire, null);
    }

    public LoadLocalDataListener(ISplitEventsManager splitEventsManager,
                                 SplitInternalEvent eventToFire,
                                 @Nullable MetadataProvider metadataProvider) {
        mSplitEventsManager = checkNotNull(splitEventsManager);
        mEventToFire = checkNotNull(eventToFire);
        mMetadataProvider = metadataProvider;
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        if (taskInfo.getStatus().equals(SplitTaskExecutionStatus.SUCCESS)) {
            EventMetadata metadata = mMetadataProvider != null ? mMetadataProvider.getMetadata() : null;
            mSplitEventsManager.notifyInternalEvent(mEventToFire, metadata);
        }
    }
}
