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

    @SerializedName("MEMBERSHIPS_LS_UPDATE")
    MEMBERSHIPS_LS_UPDATE,
    @SerializedName("MEMBERSHIPS_MS_UPDATE")
    MEMBERSHIPS_MS_UPDATE,

    @SerializedName("RULE_BASED_SEGMENT_UPDATE")
    RULE_BASED_SEGMENT_UPDATE,
}
