package io.split.android.client.service.sseclient.notifications;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Set;

import io.split.android.client.common.CompressionType;

public class MembershipNotification {

    @SerializedName("t")
    private String type;
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

    @NonNull
    public String getType() {
        return type;
    }

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
