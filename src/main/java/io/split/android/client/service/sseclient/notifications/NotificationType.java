package io.split.android.client.service.sseclient.notifications;

import com.google.gson.annotations.SerializedName;

public enum NotificationType {
    @SerializedName("SPLIT_UPDATE")
    SPLIT_UPDATE,
    @SerializedName("MY_SEGMENTS_UPDATE")
    MY_SEGMENTS_UPDATE,
    @SerializedName("MY_SEGMENTS_UPDATE_V2")
    MY_SEGMENTS_UPDATE_V2,
    @SerializedName("SPLIT_KILL")
    SPLIT_KILL,
    @SerializedName("CONTROL")
    CONTROL,
    @SerializedName("OCCUPANCY")
    OCCUPANCY,
    @SerializedName("ERROR")
    ERROR,
    @SerializedName("MY_LARGE_SEGMENT_UPDATE")
    MY_LARGE_SEGMENT_UPDATE
}
