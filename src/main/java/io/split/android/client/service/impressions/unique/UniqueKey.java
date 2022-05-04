package io.split.android.client.service.impressions.unique;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.HashSet;
import java.util.Set;

public class UniqueKey {

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
