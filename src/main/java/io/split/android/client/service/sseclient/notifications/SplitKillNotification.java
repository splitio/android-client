package io.split.android.client.service.sseclient.notifications;

public class SplitKillNotification implements IncomingNotification {
    private final NotificationType type;
    private final long changeNumber;
    private final String splitName;
    private final String defaultTreatment;

    public SplitKillNotification(NotificationType type, long changeNumber, String splitName, String defaultTreatment) {
        this.type = type;
        this.changeNumber = changeNumber;
        this.splitName = splitName;
        this.defaultTreatment = defaultTreatment;
    }

    @Override
    public NotificationType getType() {
        return type;
    }

    public long getChangeNumber() {
        return changeNumber;
    }

    public String getSplitName() {
        return splitName;
    }

    public String getDefaultTreatment() {
        return defaultTreatment;
    }
}