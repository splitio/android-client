package io.split.android.client.service.sseclient.notifications;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Set;

import io.split.android.client.common.CompressionType;

public class MyLargeSegmentChangeNotification {

    @SerializedName("changeNumber")
    private Long changeNumber;
    @SerializedName("largeSegments")
    private Set<String> largeSegments;
    @SerializedName("c")
    private CompressionType compression;
    @SerializedName("u")
    private MySegmentUpdateStrategy updateStrategy;
    @SerializedName("d")
    private String data;
    @SerializedName("i")
    private Long updateIntervalMs;
    @SerializedName("h")
    private HashingAlgorithm hashingAlgorithm;
    @SerializedName("s")
    private Integer algorithmSeed;

    @Nullable
    public Long getChangeNumber() {
        return changeNumber;
    }

    @Nullable
    public Set<String> getLargeSegments() {
        return largeSegments;
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

    public Long getUpdateIntervalMs() {
        return updateIntervalMs;
    }

    public HashingAlgorithm getHashingAlgorithm() {
        return hashingAlgorithm;
    }

    public Integer getAlgorithmSeed() {
        return algorithmSeed;
    }
}
