package io.split.android.client.service.sseclient.notifications.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.dtos.SegmentsChange;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.mysegments.MySegmentUpdateParams;
import io.split.android.client.service.mysegments.MySegmentsOverwriteTask;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeV2Notification;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.NotificationParser;

@Deprecated
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
            //noinspection ResultOfMethodCallIgnored
            mConfiguration.getNotificationsQueue().offer(new MySegmentUpdateParams(ServiceConstants.NO_INITIAL_DELAY, null, null));
        } else {
            Set<String> segmentList = notification.getSegmentList() != null ? new HashSet<>(notification.getSegmentList()) : new HashSet<>();
            MySegmentsOverwriteTask task = mConfiguration.getMySegmentsTaskFactory()
                    .createMySegmentsOverwriteTask(SegmentsChange.create(segmentList, notification.getChangeNumber()));
            mSplitTaskExecutor.submit(task, null);
        }
    }

    @Override
    public void processMySegmentsUpdateV2(MySegmentChangeV2Notification notification) {
        mProcessorHelper.processMySegmentsUpdate(notification.getUpdateStrategy(),
                notification.getData(),
                notification.getCompression(),
                Collections.singleton(notification.getSegmentName()),
                notification.getChangeNumber(),
                mConfiguration.getNotificationsQueue(),
                ServiceConstants.NO_INITIAL_DELAY);
    }
}
