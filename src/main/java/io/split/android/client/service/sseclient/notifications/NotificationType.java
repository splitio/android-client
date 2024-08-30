package io.split.android.client.service.sseclient.notifications;

import com.google.gson.annotations.SerializedName;

public enum NotificationType {
    @SerializedName("SPLIT_UPDATE")
    SPLIT_UPDATE,
    @SerializedName("SPLIT_KILL")
    SPLIT_KILL,
    @SerializedName("CONTROL")
    CONTROL,
    @SerializedName("OCCUPANCY")
    OCCUPANCY,
    @SerializedName("ERROR")
    ERROR,

    @SerializedName("MEMBERSHIP_LS_UPDATE")
    MEMBERSHIP_LS_UPDATE,
    @SerializedName("MEMBERSHIP_MS_UPDATE")
    MEMBERSHIP_MS_UPDATE,
}
