package io.split.android.client.service.impressions.unique;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.HashSet;
import java.util.Set;

import io.split.android.client.dtos.Identifiable;

public class UniqueKey implements Identifiable {

    private transient long storageId;

    @SerializedName("k")
    private final String mKey;

    @SerializedName("fs")
    private final Set<String> mFeatures;

    public UniqueKey(@NonNull String key, Set<String> features) {
        mKey = key;
        mFeatures = features;
    }

    public UniqueKey(@NonNull String key) {
        this(key, new HashSet<>());
    }

    @NonNull
    public String getKey() {
        return mKey;
    }

    @NonNull
    public Set<String> getFeatures() {
        return mFeatures;
    }

    @Override
    public long getId() {
        return storageId;
    }

    public void setStorageId(long storageId) {
        this.storageId = storageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UniqueKey uniqueKey = (UniqueKey) o;

        return mKey.equals(uniqueKey.mKey) && mFeatures.equals(uniqueKey.mFeatures);
    }

    @Override
    public int hashCode() {
        return mKey.hashCode() + mFeatures.hashCode();
    }
}
