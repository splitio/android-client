package io.split.android.client.service.sseclient.notifications.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.mysegments.MySegmentsOverwriteTask;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeV2Notification;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.NotificationParser;

public class MySegmentsNotificationProcessorImpl implements MySegmentsNotificationProcessor {

    private final MySegmentsNotificationProcessorHelper mProcessorHelper;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final MySegmentsNotificationProcessorConfiguration mConfiguration;

    public MySegmentsNotificationProcessorImpl(@NonNull NotificationParser notificationParser,
                                               @NonNull SplitTaskExecutor splitTaskExecutor,
                                               @NonNull MySegmentsV2PayloadDecoder mySegmentsPayloadDecoder,
                                               @NonNull CompressionUtilProvider compressionProvider,
                                               @NonNull MySegmentsNotificationProcessorConfiguration configuration) {
        this(new MySegmentsNotificationProcessorHelper(notificationParser, splitTaskExecutor, mySegmentsPayloadDecoder, compressionProvider, configuration), splitTaskExecutor, configuration);
    }

    @VisibleForTesting
    MySegmentsNotificationProcessorImpl(@NonNull MySegmentsNotificationProcessorHelper processorHelper,
                                        @NonNull SplitTaskExecutor splitTaskExecutor,
                                        @NonNull MySegmentsNotificationProcessorConfiguration configuration) {
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mConfiguration = checkNotNull(configuration);
        mProcessorHelper = checkNotNull(processorHelper);
    }

    @Override
    public void processMySegmentsUpdate(MySegmentChangeNotification notification) {
        if (!notification.isIncludesPayload()) {
            mConfiguration.getMySegmentUpdateNotificationsQueue().offer(notification);
        } else {
            List<String> segmentList = notification.getSegmentList() != null ? notification.getSegmentList() : new ArrayList<>();
            MySegmentsOverwriteTask task = mConfiguration.getMySegmentsTaskFactory().createMySegmentsOverwriteTask(segmentList);
            mSplitTaskExecutor.submit(task, null);
        }
    }

    @Override
    public void processMySegmentsUpdateV2(MySegmentChangeV2Notification notification) {
        mProcessorHelper.processUpdate(notification.getUpdateStrategy(),
                notification.getData(),
                notification.getCompression(),
                Collections.singleton(notification.getSegmentName()),
                mConfiguration.getMySegmentUpdateNotificationsQueue());
    }
}
