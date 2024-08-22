package io.split.android.client.dtos;

import com.google.gson.annotations.SerializedName;

public class Segment {

    @SerializedName("n")
    private String mName;

    public String getName() {
        return mName;
    }

    void setName(String name) {
        mName = name;
    }
}
