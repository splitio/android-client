package io.split.android.client.service.sseclient.notifications;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonSyntaxException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.sseclient.notifications.memberships.MembershipsNotificationProcessor;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessorRegistry;
import io.split.android.client.utils.logger.Logger;

public class NotificationProcessor implements MySegmentsNotificationProcessorRegistry {

    private final NotificationParser mNotificationParser;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final SplitTaskFactory mSplitTaskFactory;
    private final BlockingQueue<SplitsChangeNotification> mSplitsUpdateNotificationsQueue;
    private final ConcurrentMap<String, MembershipsNotificationProcessor> mMembershipsNotificationProcessors;

    public NotificationProcessor(
            @NonNull SplitTaskExecutor splitTaskExecutor,
            @NonNull SplitTaskFactory splitTaskFactory,
            @NonNull NotificationParser notificationParser,
            @NonNull BlockingQueue<SplitsChangeNotification> splitsUpdateNotificationsQueue) {
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mNotificationParser = checkNotNull(notificationParser);
        mSplitsUpdateNotificationsQueue = checkNotNull(splitsUpdateNotificationsQueue);
        mMembershipsNotificationProcessors = new ConcurrentHashMap<>();
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
                case MEMBERSHIP_MS_UPDATE:
                case MEMBERSHIP_LS_UPDATE:
                    processMembershipsUpdate(mNotificationParser.parseMembershipNotification(notificationJson));
                    break;
                default:
                    Logger.e("Unknown notification arrived: " + notificationJson);
            }
        } catch (JsonSyntaxException e) {
            Logger.e("Error processing incoming push notification: " +
                    e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("Unknown error while processing incoming push notification: " +
                    e.getLocalizedMessage());
        }
    }

    @Override
    public void registerMembershipsNotificationProcessor(String matchingKey, MembershipsNotificationProcessor processor) {
        mMembershipsNotificationProcessors.put(matchingKey, processor);
    }

    @Override
    public void unregisterMembershipsProcessor(String matchingKey) {
        mMembershipsNotificationProcessors.remove(matchingKey);
    }

    private void processSplitUpdate(SplitsChangeNotification notification) {
        Logger.d("Received split change notification");
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

    private void processMembershipsUpdate(@Nullable MembershipNotification notification) {
        for (MembershipsNotificationProcessor processor : mMembershipsNotificationProcessors.values()) {
            processor.process(notification);
        }
    }
}
