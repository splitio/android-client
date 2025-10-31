package io.split.android.client.service.sseclient.notifications;

import com.google.gson.annotations.SerializedName;

public enum HashingAlgorithm {
    @SerializedName("0")
    NONE,
    @SerializedName("1")
    MURMUR3_32,
    @SerializedName("2")
    MURMUR3_64
}
