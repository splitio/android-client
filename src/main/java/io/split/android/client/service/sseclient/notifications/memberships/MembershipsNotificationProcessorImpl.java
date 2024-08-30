package io.split.android.client.service.sseclient.notifications.memberships;

import androidx.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.common.CompressionType;
import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.mysegments.MySegmentUpdateParams;
import io.split.android.client.service.mysegments.MySegmentsUpdateTask;
import io.split.android.client.service.sseclient.notifications.KeyList;
import io.split.android.client.service.sseclient.notifications.MembershipNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentUpdateStrategy;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationType;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessorConfiguration;
import io.split.android.client.service.sseclient.notifications.mysegments.SyncDelayCalculator;
import io.split.android.client.utils.logger.Logger;

public class MembershipsNotificationProcessorImpl implements MembershipsNotificationProcessor {

    private final NotificationParser mNotificationParser;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final MySegmentsV2PayloadDecoder mMySegmentsPayloadDecoder;
    private final CompressionUtilProvider mCompressionProvider;
    private final MySegmentsNotificationProcessorConfiguration mConfiguration;
    private final SyncDelayCalculator mSyncDelayCalculator;

    public MembershipsNotificationProcessorImpl(NotificationParser notificationParser,
                                                SplitTaskExecutor splitTaskExecutor,
                                                MySegmentsV2PayloadDecoder mySegmentsPayloadDecoder,
                                                CompressionUtilProvider compressionProvider,
                                                MySegmentsNotificationProcessorConfiguration configuration,
                                                SyncDelayCalculator syncDelayCalculator) {
        mNotificationParser = notificationParser;
        mSplitTaskExecutor = splitTaskExecutor;
        mMySegmentsPayloadDecoder = mySegmentsPayloadDecoder;
        mCompressionProvider = compressionProvider;
        mConfiguration = configuration;
        mSyncDelayCalculator = syncDelayCalculator;
    }

    @Override
    public void process(@Nullable MembershipNotification notification) {
        if (notification == null) {
            notifyMySegmentRefreshNeeded(mConfiguration.getNotificationsQueue(), 0L, null, null);
        } else {
            long syncDelay = mSyncDelayCalculator.calculateSyncDelay(mConfiguration.getUserKey(),
                    notification.getUpdateIntervalMs(),
                    notification.getAlgorithmSeed(),
                    notification.getUpdateStrategy(),
                    notification.getHashingAlgorithm());

            processUpdate(notification.getType(),
                    notification.getUpdateStrategy(),
                    notification.getData(),
                    notification.getCompression(),
                    notification.getNames(),
                    notification.getChangeNumber(),
                    mConfiguration.getNotificationsQueue(),
                    syncDelay);
        }
    }

    private void processUpdate(NotificationType notificationType, MySegmentUpdateStrategy updateStrategy, String data, CompressionType compression, Set<String> names, Long changeNumber, BlockingQueue<MySegmentUpdateParams> notificationsQueue, long syncDelay) {
        try {
            switch (updateStrategy) {
                case UNBOUNDED_FETCH_REQUEST:
                    Logger.d("Received Unbounded membership fetch request");
                    notifyMySegmentRefreshNeeded(notificationsQueue, syncDelay, notificationType, changeNumber);
                    break;
                case BOUNDED_FETCH_REQUEST:
                    Logger.d("Received Bounded membership fetch request");
                    byte[] keyMap = mMySegmentsPayloadDecoder.decodeAsBytes(data,
                            mCompressionProvider.get(compression));
                    executeBoundedFetch(keyMap, syncDelay, notificationType, changeNumber);
                    break;
                case KEY_LIST:
                    Logger.d("Received KeyList membership fetch request");
                    updateSegments(notificationType, mMySegmentsPayloadDecoder.decodeAsString(data,
                                    mCompressionProvider.get(compression)),
                            names, changeNumber);
                    break;
                case SEGMENT_REMOVAL:
                    Logger.d("Received membership removal request");
                    removeSegment(notificationType, names, changeNumber);
                    break;
                default:
                    notifyMySegmentRefreshNeeded(notificationsQueue, syncDelay, notificationType, changeNumber);
                    Logger.w("Unknown membership change notification type: " + updateStrategy);
                    break;
            }
        } catch (Exception e) {
            Logger.e("Executing unbounded fetch because an error has occurred processing my "+(notificationType == NotificationType.MEMBERSHIP_LS_UPDATE ? "large" : "")+" segment notification: " + e.getLocalizedMessage());
            notifyMySegmentRefreshNeeded(notificationsQueue, syncDelay, notificationType, changeNumber);
        }
    }

    private void notifyMySegmentRefreshNeeded(BlockingQueue<MySegmentUpdateParams> notificationsQueue, long syncDelay, NotificationType notificationType, Long changeNumber) {
        Long targetSegmentsCn = (notificationType == NotificationType.MEMBERSHIP_MS_UPDATE) ? changeNumber : null;
        Long targetLargeSegmentsCn = (notificationType == NotificationType.MEMBERSHIP_LS_UPDATE) ? changeNumber : null;

        //noinspection ResultOfMethodCallIgnored
        notificationsQueue.offer(new MySegmentUpdateParams(syncDelay, targetSegmentsCn, targetLargeSegmentsCn));
    }

    private void removeSegment(NotificationType notificationType, Set<String> segmentNames, Long changeNumber) {
        // Shouldn't be null, some defensive code here
        if (segmentNames == null) {
            return;
        }
        MySegmentsUpdateTask task = (notificationType == NotificationType.MEMBERSHIP_LS_UPDATE) ?
                mConfiguration.getMySegmentsTaskFactory().createMyLargeSegmentsUpdateTask(false, segmentNames, changeNumber) :
                mConfiguration.getMySegmentsTaskFactory().createMySegmentsUpdateTask(false, segmentNames, changeNumber);
        mSplitTaskExecutor.submit(task, null);
    }

    private void executeBoundedFetch(byte[] keyMap, long syncDelay, NotificationType notificationType, Long changeNumber) {
        int index = mMySegmentsPayloadDecoder.computeKeyIndex(mConfiguration.getHashedUserKey(), keyMap.length);
        if (mMySegmentsPayloadDecoder.isKeyInBitmap(keyMap, index)) {
            Logger.d("Executing Bounded membership fetch request");
            notifyMySegmentRefreshNeeded(mConfiguration.getNotificationsQueue(), syncDelay, notificationType, changeNumber);
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

        boolean largeSegmentsUpdate = notificationType == NotificationType.MEMBERSHIP_LS_UPDATE;
        Logger.d("Executing KeyList my "+ (largeSegmentsUpdate ? "large " : "") +"segment fetch request: Adding = " + actionIsAdd);
        MySegmentsUpdateTask task = largeSegmentsUpdate ? mConfiguration.getMySegmentsTaskFactory().createMyLargeSegmentsUpdateTask(actionIsAdd, segmentNames, changeNumber) :
                mConfiguration.getMySegmentsTaskFactory().createMySegmentsUpdateTask(actionIsAdd, segmentNames, changeNumber);
        mSplitTaskExecutor.submit(task, null);
    }
}
