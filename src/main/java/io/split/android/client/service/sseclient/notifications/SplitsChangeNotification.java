package io.split.android.client.service.sseclient.notifications;

public class SplitsChangeNotification implements IncomingNotification {
    private final NotificationType type;
    private final long changeNumber;

    SplitsChangeNotification(NotificationType type, long changeNumber) {
        this.type = type;
        this.changeNumber = changeNumber;
    }

    @Override
    public NotificationType getType() {
        return null;
    }

    public long getChangeNumber() {
        return changeNumber;
    }
}

