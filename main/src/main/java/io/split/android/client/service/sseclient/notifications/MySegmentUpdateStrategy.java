package io.split.android.client.service.sseclient.notifications;

import com.google.gson.annotations.SerializedName;

public enum MySegmentUpdateStrategy {
    @SerializedName("0") UNBOUNDED_FETCH_REQUEST,
    @SerializedName("1") BOUNDED_FETCH_REQUEST,
    @SerializedName("2") KEY_LIST,
    @SerializedName("3") SEGMENT_REMOVAL
}
