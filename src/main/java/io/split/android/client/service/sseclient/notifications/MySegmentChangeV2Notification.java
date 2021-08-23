package io.split.android.client.service.sseclient.notifications;

import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.Nullable;

import io.split.android.client.common.CompressionType;

public class MySegmentChangeV2Notification extends IncomingNotification {

    private static final String FIELD_UPDATE_STRATEGY = "u";
    private static final String FIELD_COMPRESSION = "c";
    private static final String FIELD_DATE = "d";

    private Long changeNumber;
    private String segmentName;

    @SerializedName(FIELD_COMPRESSION)
    private CompressionType compression;

    @SerializedName(FIELD_UPDATE_STRATEGY)
    private MySegmentUpdateStrategy updateStrategy;

    @SerializedName(FIELD_DATE)
    private String data;

    @Nullable
    public Long getChangeNumber() {
        return changeNumber;
    }

    @Nullable
    public String getSegmentName() {
        return segmentName;
    }

    @Nullable
    public CompressionType getCompression() {
        return compression;
    }

    @Nullable
    public MySegmentUpdateStrategy getUpdateStrategy() {
        return updateStrategy;
    }

    @Nullable
    public String getData() {
        return data;
    }
}