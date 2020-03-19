package io.split.android.client.service.sseclient.notifications;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class NotificationProcessor {

    private final NotificationParser mNotificationParser;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final Map<Long, SplitsChangeNotification> mSplitUpdateNotificationBuffer;
    private final Map<Long, MySegmentChangeNotification> mMySegmentUpdateNotificationBuffer;

    public NotificationProcessor(@NonNull SplitTaskExecutor splitTaskExecutor,
                                 @NonNull NotificationParser notificationParser) {
        mSplitTaskExecutor = splitTaskExecutor;
        mNotificationParser = checkNotNull(notificationParser);
        mSplitUpdateNotificationBuffer = new ConcurrentHashMap<>();
        mMySegmentUpdateNotificationBuffer = new ConcurrentHashMap<>();
    }

    public void process(String rawJson) {
        try {
            process(mNotificationParser.parseRawNotification(rawJson).getData());
        } catch (JsonSyntaxException e) {
            Logger.e("Error processing incoming push notification: " +
                    e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("Unknown error while processing incoming push notification: " +
                    e.getLocalizedMessage());
        }
    }

    private void processIncoming(String notificationJson) throws JsonSyntaxException {
        IncomingNotification incomingNotification =
                mNotificationParser.parseIncoming(notificationJson);
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
            case CONTROL:
                processControl();
                break;
            default:
                Logger.e("Unknow notification arrived: " + notificationJson);
        }
    }

    private void processSplitUpdate(SplitsChangeNotification notification) {

    }

    private void processSplitKill(SplitKillNotification notification) {

    }

    private void processMySegmentUpdate(MySegmentChangeNotification notification) {

    }

    private void processControl() {
        // TODO: What to do here?
    }
}
