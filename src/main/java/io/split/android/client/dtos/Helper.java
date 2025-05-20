package io.split.android.client.dtos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Helper {

    public static Set<String> getReferencedRuleBasedSegments(List<Condition> conditions) {
        Set<String> referencedSegmentNames = new HashSet<>();

        if (conditions == null) {
            return referencedSegmentNames;
        }

        for (Condition condition : conditions) {
            for (Matcher matcher : condition.matcherGroup.matchers) {
                if (matcher.matcherType == MatcherType.IN_RULE_BASED_SEGMENT) {
                    referencedSegmentNames.add(matcher.userDefinedSegmentMatcherData.segmentName);
                }
            }
        }

        return referencedSegmentNames;
    }
}
