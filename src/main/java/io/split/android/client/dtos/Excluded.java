package io.split.android.client.dtos;

import com.google.gson.annotations.SerializedName;

import java.util.Set;

public class Excluded {

    @SerializedName("users")
    private Set<String> mUsers;

    @SerializedName("segments")
    private Set<String> mSegments;

    public Set<String> getSegments() {
        return mSegments;
    }

    public Set<String> getUsers() {
        return mUsers;
    }
}
