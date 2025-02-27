package io.split.android.client.dtos;

import com.google.gson.annotations.SerializedName;

import java.util.HashSet;
import java.util.Set;

public class Excluded {

    @SerializedName("keys")
    private Set<String> mKeys;

    @SerializedName("segments")
    private Set<String> mSegments;

    public Set<String> getSegments() {
        return mSegments;
    }

    public Set<String> getKeys() {
        return mKeys;
    }

    public static Excluded createEmpty() {
        Excluded excluded = new Excluded();
        excluded.mKeys = new HashSet<>();
        excluded.mSegments = new HashSet<>();
        return excluded;
    }
}
