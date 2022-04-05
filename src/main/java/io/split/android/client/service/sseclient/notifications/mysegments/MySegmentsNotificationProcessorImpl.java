package io.split.android.client.service.sseclient.notifications.mysegments;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.mysegments.MySegmentsOverwriteTask;
import io.split.android.client.service.mysegments.MySegmentsUpdateTask;
import io.split.android.client.service.sseclient.notifications.KeyList;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeV2Notification;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.utils.Logger;

public class MySegmentsNotificationProcessorImpl implements MySegmentsNotificationProcessor {

    private final NotificationParser mNotificationParser;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final MySegmentsV2PayloadDecoder mMySegmentsPayloadDecoder;
    private final CompressionUtilProvider mCompressionProvider;
    private final MySegmentsNotificationProcessorConfiguration mConfiguration;

    public MySegmentsNotificationProcessorImpl(@NonNull NotificationParser notificationParser,
                                               @NonNull SplitTaskExecutor splitTaskExecutor,
                                               @NonNull MySegmentsV2PayloadDecoder mySegmentsPayloadDecoder,
                                               @NonNull CompressionUtilProvider compressionProvider,
                                               @NonNull MySegmentsNotificationProcessorConfiguration configuration) {
        mNotificationParser = checkNotNull(notificationParser);
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mMySegmentsPayloadDecoder = checkNotNull(mySegmentsPayloadDecoder);
        mCompressionProvider = checkNotNull(compressionProvider);
        mConfiguration = checkNotNull(configuration);
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
        try {
            switch (notification.getUpdateStrategy()) {
                case UNBOUNDED_FETCH_REQUEST:
                    Logger.d("Received Unbounded my segment fetch request");
                    notifyMySegmentRefreshNeeded();
                    break;
                case BOUNDED_FETCH_REQUEST:
                    Logger.d("Received Bounded my segment fetch request");
                    byte[] keyMap = mMySegmentsPayloadDecoder.decodeAsBytes(notification.getData(),
                            mCompressionProvider.get(notification.getCompression()));
                    executeBoundedFetch(keyMap);
                    break;
                case KEY_LIST:
                    Logger.d("Received KeyList my segment fetch request");
                    updateSegments(mMySegmentsPayloadDecoder.decodeAsString(notification.getData(),
                            mCompressionProvider.get(notification.getCompression())),
                            notification.getSegmentName());
                    break;
                case SEGMENT_REMOVAL:
                    Logger.d("Received Segment removal request");
                    removeSegment(notification.getSegmentName());
                    break;
                default:
                    Logger.i("Unknown my segment change v2 notification type: " + notification.getUpdateStrategy());
            }
        } catch (Exception e) {
            Logger.e("Executing unbounded fetch because an error has occurred processing my segmentV2 notification: " + e.getLocalizedMessage());
            notifyMySegmentRefreshNeeded();
        }
    }

    private void notifyMySegmentRefreshNeeded() {
        mConfiguration.getMySegmentUpdateNotificationsQueue().offer(new MySegmentChangeNotification());
    }

    private void removeSegment(String segmentName) {
        // Shouldn't be null, some defensive code here
        if (segmentName == null) {
            return;
        }
        MySegmentsUpdateTask task = mConfiguration.getMySegmentsTaskFactory().createMySegmentsUpdateTask(false, segmentName);
        mSplitTaskExecutor.submit(task, null);
    }

    private void executeBoundedFetch(byte[] keyMap) {
        int index = mMySegmentsPayloadDecoder.computeKeyIndex(mConfiguration.getHashedUserKey(), keyMap.length);
        if (mMySegmentsPayloadDecoder.isKeyInBitmap(keyMap, index)) {
            Logger.d("Executing Unbounded my segment fetch request");
            notifyMySegmentRefreshNeeded();
        }
    }

    private void updateSegments(String keyListString, String segmentName) {
        // Shouldn't be null, some defensive code here
        if (segmentName == null) {
            return;
        }
        KeyList keyList = mNotificationParser.parseKeyList(keyListString);
        KeyList.Action action = mMySegmentsPayloadDecoder.getKeyListAction(keyList, mConfiguration.getHashedUserKey());
        boolean actionIsAdd = action != KeyList.Action.REMOVE;

        if (action == KeyList.Action.NONE) {
            return;
        }
        Logger.d("Executing KeyList my segment fetch request: Adding = " + actionIsAdd);
        MySegmentsUpdateTask task = mConfiguration.getMySegmentsTaskFactory().createMySegmentsUpdateTask(actionIsAdd, segmentName);
        mSplitTaskExecutor.submit(task, null);
    }
}
