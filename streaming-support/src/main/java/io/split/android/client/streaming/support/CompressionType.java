package io.split.android.client.streaming.support;

import com.google.gson.annotations.SerializedName;

public enum CompressionType {
    @SerializedName("0") NONE,
    @SerializedName("1") GZIP,
    @SerializedName("2") ZLIB
}
