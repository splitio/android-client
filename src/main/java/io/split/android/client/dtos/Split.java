package io.split.android.client.dtos;

import java.util.List;

import io.split.android.client.validators.Validatable;
import io.split.android.client.validators.Validator;

public class Split implements Validatable<Split>{
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

    public Split(){
    }

    public Split(String name){
        this.name = name;
    }

    @Override
    public Boolean isValid(Validator<Split> validator) {
        return validator.isValidEntity(this);
    }
}
