package io.split.android.client.service.splits;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.dtos.Split;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.streaming.UpdatesFromSSEEnum;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.logger.Logger;

public class SplitInPlaceUpdateTask implements SplitTask {

    private final SplitsStorage mSplitsStorage;
    private final long mChangeNumber;
    private final Split mSplit;
    private final SplitChangeProcessor mSplitChangeProcessor;
    private final ISplitEventsManager mEventsManager;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    public SplitInPlaceUpdateTask(@NonNull SplitsStorage splitsStorage,
                                  @NonNull SplitChangeProcessor splitChangeProcessor,
                                  @NonNull ISplitEventsManager eventsManager,
                                  @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                                  @NonNull Split split,
                                  long changeNumber) {
        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitChangeProcessor = checkNotNull(splitChangeProcessor);
        mEventsManager = checkNotNull(eventsManager);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mSplit = checkNotNull(split);
        mChangeNumber = changeNumber;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            ProcessedSplitChange processedSplitChange = mSplitChangeProcessor.process(mSplit, mChangeNumber);
            boolean triggerSdkUpdate = mSplitsStorage.update(processedSplitChange, null);

            if (triggerSdkUpdate) {
                mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
            }
            mTelemetryRuntimeProducer.recordUpdatesFromSSE(UpdatesFromSSEEnum.SPLITS);

            Logger.v("Updated feature flag");
            return SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC);
        } catch (Exception ex) {
            Logger.e("Could not update feature flag");

            return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC);
        }
    }
}
