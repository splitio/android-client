package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;

public class PushNotificationManagerDeferredStartTask implements SplitTask {

    private final PushNotificationManager mPushNotificationManager;

    public PushNotificationManagerDeferredStartTask(PushNotificationManager pushNotificationManager) {
        mPushNotificationManager = pushNotificationManager;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            mPushNotificationManager.start();
        } catch (Exception exception) {
            return SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK);
        }

        return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
    }
}
