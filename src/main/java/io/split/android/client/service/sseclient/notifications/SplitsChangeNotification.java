package io.split.android.client.service.sseclient.notifications;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import io.split.android.client.common.CompressionType;

public class SplitsChangeNotification extends IncomingNotification {

    @SerializedName("changeNumber")
    private long changeNumber;

    @SerializedName("pcn")
    @Nullable
    private Long previousChangeNumber;

    @SerializedName("d")
    @Nullable
    private String data;

    @SerializedName("c")
    @Nullable
    private Integer compressionType;

    public SplitsChangeNotification() {

    }

    public SplitsChangeNotification(long changeNumber) {
        this.changeNumber = changeNumber;
    }

    public long getChangeNumber() {
        return changeNumber;
    }

    @Nullable
    public Long getPreviousChangeNumber() {
        return previousChangeNumber;
    }

    @Nullable
    public String getData() {
        return data;
    }

    @Nullable
    public CompressionType getCompressionType() {
        if (compressionType != null) {
            if (compressionType == 0) {
                return CompressionType.NONE;
            } else if (compressionType == 1) {
                return CompressionType.GZIP;
            } else if (compressionType == 2) {
                return CompressionType.ZLIB;
            }
        }

        return null;
    }
}
