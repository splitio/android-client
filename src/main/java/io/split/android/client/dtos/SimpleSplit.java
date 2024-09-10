package io.split.android.client.dtos;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Set;

public class SimpleSplit {

    @SerializedName("name")
    public String name;

    @SerializedName("trafficTypeName")
    public String trafficTypeName;

    @Nullable
    @SerializedName("sets")
    public Set<String> sets;

    @Nullable
    public String originalJson;
}
