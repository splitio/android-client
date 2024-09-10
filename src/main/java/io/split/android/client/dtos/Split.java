package io.split.android.client.dtos;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class Split extends SimpleSplit {

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
}
