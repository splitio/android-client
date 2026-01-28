package io.split.android.client.service.sseclient.notifications;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Set;

import io.split.android.client.common.CompressionType;

public class MembershipNotification extends IncomingNotification {

    @SerializedName("cn")
    private Long changeNumber;
    @SerializedName("n")
    private Set<String> names;

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
    public Set<String> getNames() {
        return names;
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

    @Nullable
    public Long getUpdateIntervalMs() {
        return updateIntervalMs;
    }

    @Nullable
    public HashingAlgorithm getHashingAlgorithm() {
        return hashingAlgorithm;
    }

    @Nullable
    public Integer getAlgorithmSeed() {
        return algorithmSeed;
    }
}
