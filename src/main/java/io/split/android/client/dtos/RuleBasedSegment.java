package io.split.android.client.dtos;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RuleBasedSegment {

    @SerializedName("name")
    private final String mName;

    @SerializedName("trafficTypeName")
    private final String mTrafficTypeName;

    @SerializedName("changeNumber")
    private final long mChangeNumber;

    @SerializedName("status")
    private final Status mStatus;

    @SerializedName("conditions")
    private final List<Condition> mConditions;

    @SerializedName("excluded")
    private final Excluded mExcluded;

    public RuleBasedSegment(String name, String trafficTypeName, long changeNumber, Status status, List<Condition> conditions, Excluded excluded) {
        mName = name;
        mTrafficTypeName = trafficTypeName;
        mChangeNumber = changeNumber;
        mStatus = status;
        mConditions = conditions;
        mExcluded = excluded;
    }

    public String getName() {
        return mName;
    }

    public String getTrafficTypeName() {
        return mTrafficTypeName;
    }

    public long getChangeNumber() {
        return mChangeNumber;
    }

    public Status getStatus() {
        return mStatus;
    }

    public List<Condition> getConditions() {
        return mConditions;
    }

    public Excluded getExcluded() {
        return mExcluded;
    }
}
