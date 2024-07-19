package io.split.android.client.service.sseclient.notifications.mysegments;

import androidx.annotation.NonNull;

import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.notifications.MyLargeSegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.NotificationParser;

public class MyLargeSegmentsNotificationProcessorImpl implements MyLargeSegmentsNotificationProcessor {

    private final MySegmentsNotificationProcessorHelper mHelper;

    public MyLargeSegmentsNotificationProcessorImpl(@NonNull NotificationParser notificationParser,
                                                    @NonNull SplitTaskExecutor splitTaskExecutor,
                                                    @NonNull MySegmentsV2PayloadDecoder mySegmentsPayloadDecoder,
                                                    @NonNull CompressionUtilProvider compressionProvider,
                                                    @NonNull MySegmentsNotificationProcessorConfiguration configuration) {
        mHelper = new MySegmentsNotificationProcessorHelper(notificationParser, splitTaskExecutor, mySegmentsPayloadDecoder, compressionProvider, configuration);
    }

    @Override
    public void process(MyLargeSegmentChangeNotification notification) {
        MySegmentsNotificationProcessorHelper.DeferredSyncConfig deferredSyncConfig = MySegmentsNotificationProcessorHelper.DeferredSyncConfig
                .create(notification.getAlgorithmSeed(),
                        notification.getHashingAlgorithm(),
                        notification.getUpdateIntervalMs());
        mHelper.processAccordingToUpdateStrategy(notification.getUpdateStrategy(), notification.getData(), notification.getCompression(), notification.getLargeSegments(), deferredSyncConfig);
    }
}
