package io.split.android.client.dtos;

import java.util.List;
import java.util.Map;

public class Split {
    public String name;
    public int seed;
    public Status status;
    public boolean killed;
    public String defaultTreatment;
    public List<Condition> conditions;
    public String trafficTypeName;
    public long changeNumber;
    public Integer trafficAllocation;
    public Integer trafficAllocationSeed;
    public int algo;
    public Map<String, String> configurations;
}
