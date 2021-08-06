package io.split.android.client.service.sseclient.notifications;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MySegmentChangeV2Notification extends IncomingNotification {

    public enum Type {
        UNBOUNDED_FETCH_REQUEST, BOUNDED_FETCH_REQUEST, KEY_LIST, SEGMENT_REMOVAL
    }

    public enum CompressionType {
        NONE, GZIP, ZLIB
    }

    private Long changeNumber;
    private String segmentName;
    private MySegmentChangeV2Notification.CompressionType compression;
    private MySegmentChangeV2Notification.Type envScopedType;
    private String[] data;

    @Nullable
    public Long getChangeNumber() {
        return changeNumber;
    }

    @Nullable
    public String getSegmentName() {
        return segmentName;
    }

    @Nullable
    public MySegmentChangeV2Notification.CompressionType getCompression() {
        return compression;
    }

    @Nullable
    public MySegmentChangeV2Notification.Type getEnvScopedType() {
        return envScopedType;
    }

    @Nullable
    public String[] getData() {
        return data;
    }
}