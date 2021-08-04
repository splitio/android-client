package io.split.android.client.service.sseclient.notifications;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.mysegments.MySegmentsUpdateTask;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class NotificationProcessor {

    private final NotificationParser mNotificationParser;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final SplitTaskFactory mSplitTaskFactory;
    private final BlockingQueue<MySegmentChangeNotification> mMySegmentUpdateNotificationsQueue;
    private final BlockingQueue<SplitsChangeNotification> mSplitsUpdateNotificationsQueue;

    public NotificationProcessor(
            @NonNull SplitTaskExecutor splitTaskExecutor,
            @NonNull SplitTaskFactory splitTaskFactory,
            @NonNull NotificationParser notificationParser,
            @NonNull BlockingQueue<MySegmentChangeNotification> mySegmentUpdateNotificationsQueue,
            @NonNull BlockingQueue<SplitsChangeNotification> splitsUpdateNotificationsQueue) {
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mNotificationParser = checkNotNull(notificationParser);
        mMySegmentUpdateNotificationsQueue = checkNotNull(mySegmentUpdateNotificationsQueue);
        mSplitsUpdateNotificationsQueue = checkNotNull(splitsUpdateNotificationsQueue);
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
            MySegmentsUpdateTask task = mSplitTaskFactory.createMySegmentsUpdateTask(segmentList);
            mSplitTaskExecutor.submit(task, null);
        }
    }

    private void processMySegmentUpdateV2(MySegmentChangeV2Notification notification) {

        switch (notification.getEnvScopedType()) {
            case UNBOUNDED_FETCH_REQUEST:
                MySegmentsSyncTask task = mSplitTaskFactory.createMySegmentsSyncTask(true);
                mSplitTaskExecutor.submit(task, null);
                break;
            case BOUNDED_FETCH_REQUEST:
                break;
            case KEY_LIST:
                break;
            case SEGMENT_REMOVAL:
                break;
            default:
                Logger.i("Unknown my segment change v2 notification type: " + notification.getEnvScopedType());
        }
    }
}
