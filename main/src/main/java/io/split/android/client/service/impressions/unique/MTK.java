package io.split.android.client.service.impressions.unique;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class MTK {

    @SerializedName("keys")
    private final List<UniqueKey> mKeys;

    public MTK(List<UniqueKey> keys) {
        mKeys = keys == null ? new ArrayList<>() : keys;
    }

    public MTK() {
        this(new ArrayList<>());
    }

    @NonNull
    public List<UniqueKey> getKeys() {
        return mKeys;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MTK mtk = (MTK) o;

        return mKeys.equals(mtk.mKeys);
    }

    @Override
    public int hashCode() {
        return mKeys.hashCode();
    }
}
