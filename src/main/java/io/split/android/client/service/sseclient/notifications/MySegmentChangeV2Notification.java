package io.split.android.client.service.sseclient.notifications;

import org.jetbrains.annotations.Nullable;

import io.split.android.client.common.CompressionType;

public class MySegmentChangeV2Notification extends IncomingNotification {

    public enum Type {
        UNBOUNDED_FETCH_REQUEST, BOUNDED_FETCH_REQUEST, KEY_LIST, SEGMENT_REMOVAL
    }

    private Long changeNumber;
    private String segmentName;
    private CompressionType compression;
    private MySegmentChangeV2Notification.Type envScopedType;
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
    public MySegmentChangeV2Notification.Type getEnvScopedType() {
        return envScopedType;
    }

    @Nullable
    public String getData() {
        return data;
    }
}