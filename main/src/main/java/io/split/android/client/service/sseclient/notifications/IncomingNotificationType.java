package io.split.android.client.service.sseclient.notifications;

import com.google.gson.annotations.SerializedName;

public class IncomingNotificationType {
    @SerializedName(value = "type")
    protected NotificationType type;

    public NotificationType getType() {
        return type;
    }
}
