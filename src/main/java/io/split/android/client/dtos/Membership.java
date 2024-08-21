package io.split.android.client.dtos;

import com.google.gson.annotations.SerializedName;

public class Membership {

    @SerializedName("n")
    private String mName;

    String getName() {
        return mName;
    }

    void setName(String name) {
        mName = name;
    }
}
