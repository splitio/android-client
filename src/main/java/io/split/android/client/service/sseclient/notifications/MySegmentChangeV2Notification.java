package io.split.android.client.service.sseclient.notifications;

import org.jetbrains.annotations.Nullable;

import io.split.android.client.common.CompressionType;

public class MySegmentChangeV2Notification extends IncomingNotification {

    private Long changeNumber;
    private String segmentName;
    private CompressionType compression;
    private MySegmentUpdateStrategy updateStrategy;
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