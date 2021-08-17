package io.split.android.client.service.sseclient.notifications;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.dtos.Split;
import io.split.android.client.exceptions.MySegmentsParsingException;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.mysegments.MySegmentsUpdateTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.mysegments.MySegmentsOverwriteTask;
import io.split.android.client.utils.CompressionUtil;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.StringHelper;

import static com.google.common.base.Preconditions.checkNotNull;

public class NotificationProcessor {

    private final NotificationParser mNotificationParser;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final SplitTaskFactory mSplitTaskFactory;
    private final BlockingQueue<MySegmentChangeNotification> mMySegmentUpdateNotificationsQueue;
    private final BlockingQueue<SplitsChangeNotification> mSplitsUpdateNotificationsQueue;
    private final BigInteger mHashedUserKey;
    private final MySegmentsV2PayloadDecoder mMySegmentsPayloadDecoder;
    private final CompressionUtilProvider mCompressionProvider;

    public NotificationProcessor(
            @NonNull String userKey,
            @NonNull SplitTaskExecutor splitTaskExecutor,
            @NonNull SplitTaskFactory splitTaskFactory,
            @NonNull NotificationParser notificationParser,
            @NonNull MySegmentsV2PayloadDecoder mySegmentsPayloadDecoder,
            @NonNull CompressionUtilProvider compressionUtilProvider,
            @NonNull BlockingQueue<MySegmentChangeNotification> mySegmentUpdateNotificationsQueue,
            @NonNull BlockingQueue<SplitsChangeNotification> splitsUpdateNotificationsQueue) {
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mNotificationParser = checkNotNull(notificationParser);
        mCompressionProvider = checkNotNull(compressionUtilProvider);
        mMySegmentsPayloadDecoder = checkNotNull(mySegmentsPayloadDecoder);
        mMySegmentUpdateNotificationsQueue = checkNotNull(mySegmentUpdateNotificationsQueue);
        mSplitsUpdateNotificationsQueue = checkNotNull(splitsUpdateNotificationsQueue);
        mHashedUserKey = mySegmentsPayloadDecoder.hashKey(userKey);
    }

    public void process(IncomingNotification incomingNotification) {
        try {
            String notificationJson = incomingNotification.getJsonData();
            switch (incomingNotification.getType()) {
                case SPLIT_UPDATE:
                    processSplitUpdate(mNotificationParser.parseSplitUpdate(notificationJson));
                    break;
                case SPLIT_KILL:
                    processSplitKill(mNotificationParser.parseSplitKill(notificationJson));
                    break;
                case MY_SEGMENTS_UPDATE:
                    processMySegmentUpdate(mNotificationParser.parseMySegmentUpdate(notificationJson));
                    break;
                case MY_SEGMENTS_UPDATE_V2:
                    processMySegmentUpdateV2(mNotificationParser.parseMySegmentUpdateV2(notificationJson));
                    break;
                default:
                    Logger.e("Unknow notification arrived: " + notificationJson);
            }
        } catch (JsonSyntaxException e) {
            Logger.e("Error processing incoming push notification: " +
                    e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("Unknown error while processing incoming push notification: " +
                    e.getLocalizedMessage());
        }
    }

    private void processSplitUpdate(SplitsChangeNotification notification) {
        mSplitsUpdateNotificationsQueue.offer(notification);
    }

    private void processSplitKill(SplitKillNotification notification) {
        Split split = new Split();
        split.name = notification.getSplitName();
        split.defaultTreatment = notification.getDefaultTreatment();
        split.changeNumber = notification.getChangeNumber();
        mSplitTaskExecutor.submit(mSplitTaskFactory.createSplitKillTask(split), null);
        mSplitsUpdateNotificationsQueue.offer(new SplitsChangeNotification(split.changeNumber));
    }

    private void processMySegmentUpdate(MySegmentChangeNotification notification) {
        if (!notification.isIncludesPayload()) {
            mMySegmentUpdateNotificationsQueue.offer(notification);
        } else {
            List<String> segmentList = notification.getSegmentList() != null ? notification.getSegmentList() : new ArrayList<>();
            MySegmentsOverwriteTask task = mSplitTaskFactory.createMySegmentsOverwriteTask(segmentList);
            mSplitTaskExecutor.submit(task, null);
        }
    }

    private void processMySegmentUpdateV2(MySegmentChangeV2Notification notification) {
        try {
            switch (notification.getEnvScopedType()) {
                case UNBOUNDED_FETCH_REQUEST:
                    Logger.d("Received Unbounded my segment fetch request");
                    executeUnboundedFetch();
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
                    Logger.i("Unknown my segment change v2 notification type: " + notification.getEnvScopedType());
            }
        } catch (Exception e) {
            Logger.e("Executing unbounded fetch because an error has occurred processing my segmentV2 notification: " + e.getLocalizedMessage());
            executeUnboundedFetch();
        }
    }

    private void executeUnboundedFetch() {
        MySegmentsSyncTask task = mSplitTaskFactory.createMySegmentsSyncTask(true);
        mSplitTaskExecutor.submit(task, null);
    }

    private void removeSegment(String segmentName) {
        // Shouldn't be null, some defensive code here
        if (segmentName == null) {
            return;
        }
        MySegmentsUpdateTask task = mSplitTaskFactory.createMySegmentsUpdateTask(false, segmentName);
        mSplitTaskExecutor.submit(task, null);
    }

    private void executeBoundedFetch(byte[] keyMap) {
        int index = mMySegmentsPayloadDecoder.computeKeyIndex(mHashedUserKey, keyMap.length);
        if (mMySegmentsPayloadDecoder.isKeyInBitmap(keyMap, index)) {
            Logger.d("Executing Unbounded my segment fetch request");
            MySegmentsSyncTask task = mSplitTaskFactory.createMySegmentsSyncTask(true);
            mSplitTaskExecutor.submit(task, null);
        }
    }

    private void updateSegments(String keyListString, String segmentName) {
        // Shouldn't be null, some defensive code here
        if (segmentName == null) {
            return;
        }
        KeyList keyList = mNotificationParser.parseKeyList(keyListString);
        KeyList.Action action = mMySegmentsPayloadDecoder.getKeyListAction(keyList, mHashedUserKey);
        boolean actionIsAdd = true;

        if (action == KeyList.Action.REMOVE) {
            actionIsAdd = false;
        }
        if (action == KeyList.Action.NONE) {
            return;
        }
        Logger.d("Executing KeyList my segment fetch request: Adding = " + actionIsAdd);
        MySegmentsUpdateTask task = mSplitTaskFactory.createMySegmentsUpdateTask(actionIsAdd, segmentName);
        mSplitTaskExecutor.submit(task, null);
    }
}
