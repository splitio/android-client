package io.split.android.engine.matchers;

import static io.split.android.client.utils.Utils.checkNotNull;

import java.util.Map;

import io.split.android.client.Evaluator;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.engine.experiments.ParsedCondition;
import io.split.android.engine.experiments.ParsedRuleBasedSegment;

public class InRuleBasedSegmentMatcher implements Matcher {

    private final RuleBasedSegmentStorage mRuleBasedSegmentStorage;
    private final MySegmentsStorage mMySegmentsStorage;
    private final String mSegmentName;

    public InRuleBasedSegmentMatcher(RuleBasedSegmentStorage ruleBasedSegmentStorage, MySegmentsStorage mySegmentsStorage, String segmentName) {
        mRuleBasedSegmentStorage = checkNotNull(ruleBasedSegmentStorage);
        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
        mSegmentName = checkNotNull(segmentName);
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        if (!(matchValue instanceof String)) {
            return false;
        }

        final String matchingKey = (String) matchValue;
        final ParsedRuleBasedSegment parsedRuleBasedSegment = mRuleBasedSegmentStorage.get(mSegmentName, matchingKey);

        if (parsedRuleBasedSegment == null) {
            return false;
        }

        if (parsedRuleBasedSegment.getExcludedKeys().contains(matchingKey)) {
            return false;
        }

        for (String segmentName : parsedRuleBasedSegment.getExcludedSegments()) {
            if (mMySegmentsStorage.getAll().contains(segmentName)) {
                return false;
            }
        }

        for (ParsedCondition condition : parsedRuleBasedSegment.getParsedConditions()) {
            if (condition.matcher().match(matchingKey, bucketingKey, attributes, evaluator)) {
                return true;
            }
        }

        return false;
    }
}
