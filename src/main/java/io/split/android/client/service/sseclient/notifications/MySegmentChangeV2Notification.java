package io.split.android.client.service.sseclient.notifications;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MySegmentChangeV2Notification extends IncomingNotification {
    private Long changeNumber;
    private String segmentName;
    private Long compression;
    private Long envScopedType;
    private byte[] data;

    @Nullable
    public Long getChangeNumber() {
        return changeNumber;
    }

    @Nullable
    public String getSegmentName() {
        return segmentName;
    }

    @Nullable
    public Long getCompression() {
        return compression;
    }

    @Nullable
    public Long getEnvScopedType() {
        return envScopedType;
    }

    @Nullable
    public byte[] getData() {
        return data;
    }
}