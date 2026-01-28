package io.split.android.client.service.sseclient.notifications;

import com.google.gson.annotations.SerializedName;

public class SplitKillNotification extends IncomingNotification {
    @SerializedName("changeNumber")
    private long changeNumber;
    @SerializedName("splitName")
    private String splitName;
    @SerializedName("defaultTreatment")
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
