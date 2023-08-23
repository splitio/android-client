package io.split.android.engine.experiments;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ParsedSplit {

    private final String mSplit;
    private final int mSeed;
    private final boolean mKilled;
    private final String mDefaultTreatment;
    private final ImmutableList<ParsedCondition> mParsedCondition;
    private final String mTrafficTypeName;
    private final long mChangeNumber;
    private final int mTrafficAllocation;
    private final int mTrafficAllocationSeed;
    private final int mAlgo;
    private final Map<String, String> mConfigurations;
    private final Set<String> mSets;

    public ParsedSplit(
            String feature,
            int seed,
            boolean killed,
            String defaultTreatment,
            List<ParsedCondition> matcherAndSplits,
            String trafficTypeName,
            long changeNumber,
            int trafficAllocation,
            int trafficAllocationSeed,
            int algo,
            Map<String, String> configurations,
            Set<String> sets
    ) {
        mSplit = feature;
        mSeed = seed;
        mKilled = killed;
        mDefaultTreatment = defaultTreatment;
        mParsedCondition = ImmutableList.copyOf(matcherAndSplits);
        mTrafficTypeName = trafficTypeName;
        mChangeNumber = changeNumber;
        mAlgo = algo;
        mConfigurations = configurations;

        if (mDefaultTreatment == null) {
            throw new IllegalArgumentException("DefaultTreatment is null");
        }
        mTrafficAllocation = trafficAllocation;
        mTrafficAllocationSeed = trafficAllocationSeed;
        mSets = sets;
    }

    public String feature() {
        return mSplit;
    }

    public int trafficAllocation() {
        return mTrafficAllocation;
    }

    public int trafficAllocationSeed() {
        return mTrafficAllocationSeed;
    }

    public int seed() {
        return mSeed;
    }

    public boolean killed() {
        return mKilled;
    }

    public String defaultTreatment() {
        return mDefaultTreatment;
    }

    public List<ParsedCondition> parsedConditions() {
        return mParsedCondition;
    }

    public String trafficTypeName() {
        return mTrafficTypeName;
    }

    public long changeNumber() {
        return mChangeNumber;
    }

    public int algo() {
        return mAlgo;
    }

    public Map<String, String> configurations() {
        return mConfigurations;
    }

    public Set<String> sets() {
        return mSets;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + mSplit.hashCode();
        result = 31 * result + (int) (mSeed ^ (mSeed >>> 32));
        result = 31 * result + (mKilled ? 1 : 0);
        result = 31 * result + mDefaultTreatment.hashCode();
        result = 31 * result + mParsedCondition.hashCode();
        result = 31 * result + (mTrafficTypeName == null ? 0 : mTrafficTypeName.hashCode());
        result = 31 * result + (int) (mChangeNumber ^ (mChangeNumber >>> 32));
        result = 31 * result + (mAlgo ^ (mAlgo >>> 32));
        result = 31 * result + ((mSets != null) ? mSets.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof ParsedSplit)) return false;

        ParsedSplit other = (ParsedSplit) obj;
        return mSplit.equals(other.mSplit)
                && mSeed == other.mSeed
                && mKilled == other.mKilled
                && mDefaultTreatment.equals(other.mDefaultTreatment)
                && mParsedCondition.equals(other.mParsedCondition)
                && (Objects.equals(mTrafficTypeName, other.mTrafficTypeName))
                && mChangeNumber == other.mChangeNumber
                && mAlgo == other.mAlgo
                && (Objects.equals(mConfigurations, other.mConfigurations))
                && (Objects.equals(mSets, other.mSets));

    }

    @NonNull
    @Override
    public String toString() {
        return "name:" + mSplit + ", seed:" + mSeed + ", killed:" + mKilled +
                ", default treatment:" + mDefaultTreatment +
                ", parsedConditions:" + mParsedCondition +
                ", trafficTypeName:" + mTrafficTypeName + ", changeNumber:" + mChangeNumber +
                ", algo:" + mAlgo + ", config:" + mConfigurations + ", sets:" + mSets;

    }
}
