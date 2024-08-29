package io.split.android.client.service.sseclient.notifications.mysegments;

import java.util.Set;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.common.CompressionType;
import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.mysegments.MySegmentUpdateParams;
import io.split.android.client.service.mysegments.MySegmentsUpdateTask;
import io.split.android.client.service.sseclient.notifications.KeyList;
import io.split.android.client.service.sseclient.notifications.MySegmentUpdateStrategy;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationType;
import io.split.android.client.utils.logger.Logger;

class MySegmentsNotificationProcessorHelper {

    private final NotificationParser mNotificationParser;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final MySegmentsV2PayloadDecoder mMySegmentsPayloadDecoder;
    private final CompressionUtilProvider mCompressionProvider;
    private final MySegmentsNotificationProcessorConfiguration mConfiguration;

    public MySegmentsNotificationProcessorHelper(NotificationParser notificationParser,
                                                 SplitTaskExecutor splitTaskExecutor,
                                                 MySegmentsV2PayloadDecoder mySegmentsPayloadDecoder,
                                                 CompressionUtilProvider compressionProvider,
                                                 MySegmentsNotificationProcessorConfiguration configuration) {
        mNotificationParser = notificationParser;
        mSplitTaskExecutor = splitTaskExecutor;
        mMySegmentsPayloadDecoder = mySegmentsPayloadDecoder;
        mCompressionProvider = compressionProvider;
        mConfiguration = configuration;
    }

    void processMySegmentsUpdate(MySegmentUpdateStrategy updateStrategy, String data, CompressionType compression, Set<String> segmentNames, Long changeNumber, BlockingQueue<MySegmentUpdateParams> notificationsQueue, long syncDelay) {
        processUpdate(NotificationType.MY_SEGMENTS_UPDATE_V2, updateStrategy, data, compression, segmentNames, changeNumber, notificationsQueue, syncDelay);
    }

    void processMyLargeSegmentsUpdate(MySegmentUpdateStrategy updateStrategy, String data, CompressionType compression, Set<String> segmentNames, Long changeNumber, BlockingQueue<MySegmentUpdateParams> notificationsQueue, long syncDelay) {
        processUpdate(NotificationType.MY_LARGE_SEGMENT_UPDATE, updateStrategy, data, compression, segmentNames, changeNumber, notificationsQueue, syncDelay);
    }

    private void processUpdate(NotificationType notificationType, MySegmentUpdateStrategy updateStrategy, String data, CompressionType compression, Set<String> segmentNames, Long changeNumber, BlockingQueue<MySegmentUpdateParams> notificationsQueue, long syncDelay) {
        try {
            switch (updateStrategy) {
                case UNBOUNDED_FETCH_REQUEST:
                    Logger.d("Received Unbounded my segment fetch request");
                    notifyMySegmentRefreshNeeded(notificationsQueue, syncDelay, notificationType, changeNumber);
                    break;
                case BOUNDED_FETCH_REQUEST:
                    Logger.d("Received Bounded my segment fetch request");
                    byte[] keyMap = mMySegmentsPayloadDecoder.decodeAsBytes(data,
                            mCompressionProvider.get(compression));
                    executeBoundedFetch(keyMap, syncDelay);
                    break;
                case KEY_LIST:
                    Logger.d("Received KeyList my segment fetch request");
                    updateSegments(notificationType, mMySegmentsPayloadDecoder.decodeAsString(data,
                                    mCompressionProvider.get(compression)),
                            segmentNames, changeNumber);
                    break;
                case SEGMENT_REMOVAL:
                    Logger.d("Received Segment removal request");
                    removeSegment(notificationType, segmentNames, changeNumber);
                    break;
                default:
                    Logger.i("Unknown my segment change v2 notification type: " + updateStrategy);
            }
        } catch (Exception e) {
            Logger.e("Executing unbounded fetch because an error has occurred processing my "+(notificationType == NotificationType.MY_LARGE_SEGMENT_UPDATE ? "large" : "")+" segment notification: " + e.getLocalizedMessage());
            notifyMySegmentRefreshNeeded(notificationsQueue, syncDelay, notificationType, changeNumber);
        }
    }

    private void notifyMySegmentRefreshNeeded(BlockingQueue<MySegmentUpdateParams> notificationsQueue, long syncDelay, NotificationType notificationType, Long changeNumber) {
        Long targetSegmentsCn = (notificationType == NotificationType.MY_LARGE_SEGMENT_UPDATE) ? null : changeNumber;
        Long targetLargeSegmentsCn = (notificationType == NotificationType.MY_LARGE_SEGMENT_UPDATE) ? changeNumber : null;

        //noinspection ResultOfMethodCallIgnored
        notificationsQueue.offer(new MySegmentUpdateParams(syncDelay, targetSegmentsCn, targetLargeSegmentsCn));
    }

    private void removeSegment(NotificationType notificationType, Set<String> segmentNames, Long changeNumber) {
        // Shouldn't be null, some defensive code here
        if (segmentNames == null) {
            return;
        }
        MySegmentsUpdateTask task = (notificationType == NotificationType.MY_LARGE_SEGMENT_UPDATE) ?
                mConfiguration.getMySegmentsTaskFactory().createMyLargeSegmentsUpdateTask(false, segmentNames, changeNumber) :
                mConfiguration.getMySegmentsTaskFactory().createMySegmentsUpdateTask(false, segmentNames, changeNumber);
        mSplitTaskExecutor.submit(task, null);
    }

    private void executeBoundedFetch(byte[] keyMap, long syncDelay) {
        int index = mMySegmentsPayloadDecoder.computeKeyIndex(mConfiguration.getHashedUserKey(), keyMap.length);
        if (mMySegmentsPayloadDecoder.isKeyInBitmap(keyMap, index)) {
            Logger.d("Executing Bounded my segment fetch request");
            notifyMySegmentRefreshNeeded(mConfiguration.getMySegmentUpdateNotificationsQueue(), syncDelay, NotificationType.MY_SEGMENTS_UPDATE_V2, null); // TODO
        }
    }

    private void updateSegments(NotificationType notificationType, String keyListString, Set<String> segmentNames, Long changeNumber) {
        // Shouldn't be null, some defensive code here
        if (segmentNames == null) {
            return;
        }
        KeyList keyList = mNotificationParser.parseKeyList(keyListString);
        KeyList.Action action = mMySegmentsPayloadDecoder.getKeyListAction(keyList, mConfiguration.getHashedUserKey());
        boolean actionIsAdd = action != KeyList.Action.REMOVE;

        if (action == KeyList.Action.NONE) {
            return;
        }

        boolean largeSegmentsUpdate = notificationType == NotificationType.MY_LARGE_SEGMENT_UPDATE;
        Logger.d("Executing KeyList my "+ (largeSegmentsUpdate ? "large " : "") +"segment fetch request: Adding = " + actionIsAdd);
        MySegmentsUpdateTask task = largeSegmentsUpdate ? mConfiguration.getMySegmentsTaskFactory().createMyLargeSegmentsUpdateTask(actionIsAdd, segmentNames, changeNumber) :
            mConfiguration.getMySegmentsTaskFactory().createMySegmentsUpdateTask(actionIsAdd, segmentNames, changeNumber);
        mSplitTaskExecutor.submit(task, null);
    }
}
