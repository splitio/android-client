package io.split.android.client.dtos;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Split {

    @SerializedName("name")
    public String name;

    @SerializedName("seed")
    public int seed;

    @SerializedName("status")
    public Status status;

    @SerializedName("killed")
    public boolean killed;

    @SerializedName("defaultTreatment")
    public String defaultTreatment;

    @SerializedName("conditions")
    public List<Condition> conditions;

    @SerializedName("trafficTypeName")
    public String trafficTypeName;

    @SerializedName("changeNumber")
    public long changeNumber;

    @SerializedName("trafficAllocation")
    public Integer trafficAllocation;

    @SerializedName("trafficAllocationSeed")
    public Integer trafficAllocationSeed;

    @SerializedName("algo")
    public int algo;

    @SerializedName("configurations")
    public Map<String, String> configurations;

    @Nullable
    @SerializedName("sets")
    public Set<String> sets;
}
