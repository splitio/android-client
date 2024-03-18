package io.split.android.client.validators;

import androidx.annotation.NonNull;

import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.utils.logger.Logger;

class ImpressionLoggingTask implements SplitTask {

    private final ImpressionListener mImpressionListener;
    private final Impression mImpression;

    ImpressionLoggingTask(ImpressionListener impressionListener,
                          Impression impression) {
        mImpressionListener = impressionListener;
        mImpression = impression;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            mImpressionListener.log(mImpression);
        } catch (Throwable t) {
            Logger.e("An error occurred logging impression: " + t.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK);
        }

        return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
    }
}
