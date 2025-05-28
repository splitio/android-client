package io.split.android.engine.experiments;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.split.android.client.dtos.ExcludedSegment;

public class ParsedRuleBasedSegment {
    private final String mName;
    private final Set<String> mExcludedKeys;
    private final Set<ExcludedSegment> mExcludedSegments;
    private final List<ParsedCondition> mParsedConditions;
    private final String mTrafficTypeName;
    private final long mChangeNumber;

    public ParsedRuleBasedSegment(String name, Set<String> excludedKeys, Set<ExcludedSegment> excludedSegments, List<ParsedCondition> parsedConditions, String trafficTypeName, long changeNumber) {
        mName = name;
        mExcludedKeys = excludedKeys == null ? new HashSet<>() : excludedKeys;
        mExcludedSegments = excludedSegments == null ? new HashSet<>() : excludedSegments;
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

    public Set<ExcludedSegment> getExcludedSegments() {
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