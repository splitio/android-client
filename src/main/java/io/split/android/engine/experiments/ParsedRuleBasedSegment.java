package io.split.android.engine.experiments;

import java.util.List;
import java.util.Set;

public class ParsedRuleBasedSegment {
    private final String mName;
    private final Set<String> mExcludedKeys;
    private final Set<String> mExcludedSegments;
    private final List<ParsedCondition> mParsedConditions;
    private final String mTrafficTypeName;
    private final long mChangeNumber;

    public ParsedRuleBasedSegment(String name, Set<String> excludedKeys, Set<String> excludedSegments, List<ParsedCondition> parsedConditions, String trafficTypeName, long changeNumber) {
        mName = name;
        mExcludedKeys = excludedKeys;
        mExcludedSegments = excludedSegments;
        mParsedConditions = parsedConditions;
        mTrafficTypeName = trafficTypeName;
        mChangeNumber = changeNumber;
    }

    public String getName() {
        return mName;
    }

    public Set<String> getExcludedKeys() {
        return mExcludedKeys;
    }

    public Set<String> getExcludedSegments() {
        return mExcludedSegments;
    }

    public List<ParsedCondition> getParsedConditions() {
        return mParsedConditions;
    }

    public String getTrafficTypeName() {
        return mTrafficTypeName;
    }

    public long getChangeNumber() {
        return mChangeNumber;
    }
}