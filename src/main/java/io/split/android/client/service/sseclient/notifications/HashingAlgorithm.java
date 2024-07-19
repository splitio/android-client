package io.split.android.client.service.sseclient.notifications;

import com.google.gson.annotations.SerializedName;

public enum HashingAlgorithm {
    @SerializedName("0")
    MURMUR3_32,
    @SerializedName("1")
    MURMUR3_64
}
