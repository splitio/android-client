package io.split.android.client.service.sseclient.notifications;

public class SplitKillNotification extends IncomingNotification {
    private long changeNumber;
    private String splitName;
    private String defaultTreatment;

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