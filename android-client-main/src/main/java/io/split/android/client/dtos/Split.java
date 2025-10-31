package io.split.android.client.dtos;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
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

    @SerializedName("impressionsDisabled")
    public boolean impressionsDisabled = false;

    @Nullable
    @SerializedName("prerequisites")
    public List<Prerequisite> prerequisites;

    public String json = null;

    public Split() {

    }

    public Split(String name, String json) {
        this.name = name;
        this.json = json;
    }

    public List<Prerequisite> getPrerequisites() {
        return prerequisites == null ? new ArrayList<>() : prerequisites;
    }
}
