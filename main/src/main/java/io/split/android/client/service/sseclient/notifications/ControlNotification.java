package io.split.android.client.service.sseclient.notifications;

import com.google.gson.annotations.SerializedName;

public class ControlNotification extends IncomingNotification {
    public enum ControlType {
        @SerializedName("STREAMING_RESUMED")
        STREAMING_RESUMED,
        @SerializedName("STREAMING_DISABLED")
        STREAMING_DISABLED,
        @SerializedName("STREAMING_PAUSED")
        STREAMING_PAUSED,
        @SerializedName("STREAMING_RESET")
        STREAMING_RESET
    }

    @SuppressWarnings("unused")
    @SerializedName("controlType")
    private ControlType controlType;

    public ControlType getControlType() {
        return controlType;
    }

    public void setTimestamp(long timestamp) {
        super.timestamp = timestamp;
    }
}
