package io.split.android.client.service.sseclient.notifications.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.BlockingQueue;

import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.mysegments.MySegmentUpdateParams;
import io.split.android.client.service.sseclient.notifications.MyLargeSegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.NotificationParser;

public class MyLargeSegmentsNotificationProcessorImpl implements MyLargeSegmentsNotificationProcessor {

    private final MySegmentsNotificationProcessorHelper mProcessorHelper;
    private final String mUserKey;
    private final BlockingQueue<MySegmentUpdateParams> mNotificationQueue;
    private final SyncDelayCalculator mSyncDelayCalculator;

    public MyLargeSegmentsNotificationProcessorImpl(@NonNull NotificationParser notificationParser,
                                                    @NonNull SplitTaskExecutor splitTaskExecutor,
                                                    @NonNull MySegmentsV2PayloadDecoder mySegmentsPayloadDecoder,
                                                    @NonNull CompressionUtilProvider compressionProvider,
                                                    @NonNull MySegmentsNotificationProcessorConfiguration configuration) {
        this(new MySegmentsNotificationProcessorHelper(notificationParser, splitTaskExecutor, mySegmentsPayloadDecoder, compressionProvider, configuration),
                configuration, new SyncDelayCalculatorImpl());
    }

    @VisibleForTesting
    MyLargeSegmentsNotificationProcessorImpl(@NonNull MySegmentsNotificationProcessorHelper processorHelper,
                                             @NonNull MySegmentsNotificationProcessorConfiguration configuration,
                                             @NonNull SyncDelayCalculator syncDelayCalculator) {
        mProcessorHelper = checkNotNull(processorHelper);
        mUserKey = configuration.getUserKey();
        mNotificationQueue = configuration.getMySegmentUpdateNotificationsQueue();
        mSyncDelayCalculator = checkNotNull(syncDelayCalculator);
    }

    @Override
    public void process(@NonNull MyLargeSegmentChangeNotification notification) {
        long syncDelay = mSyncDelayCalculator.calculateSyncDelay(mUserKey,
                notification.getUpdateIntervalMs(),
                notification.getAlgorithmSeed(),
                notification.getUpdateStrategy(),
                notification.getHashingAlgorithm());

        mProcessorHelper.processMyLargeSegmentsUpdate(
                notification.getUpdateStrategy(),
                notification.getData(),
                notification.getCompression(),
                notification.getLargeSegments(),
                notification.getChangeNumber(),
                mNotificationQueue,
                syncDelay);
    }
}
