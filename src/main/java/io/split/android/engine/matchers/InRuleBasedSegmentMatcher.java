package io.split.android.engine.matchers;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.Map;

import io.split.android.client.Evaluator;
import io.split.android.client.dtos.ExcludedSegment;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorageConsumer;
import io.split.android.engine.experiments.ParsedCondition;
import io.split.android.engine.experiments.ParsedRuleBasedSegment;

public class InRuleBasedSegmentMatcher implements Matcher {

    private final RuleBasedSegmentStorageConsumer mRuleBasedSegmentStorage;
    private final MySegmentsStorage mMySegmentsStorage;
    private final MySegmentsStorage mMyLargeSegmentsStorage;
    private final String mSegmentName;

    public InRuleBasedSegmentMatcher(@NonNull RuleBasedSegmentStorageConsumer ruleBasedSegmentStorage,
                                     @NonNull MySegmentsStorage mySegmentsStorage,
                                     @NonNull MySegmentsStorage myLargeSegmentsStorage,
                                     @NonNull String segmentName) {
        mRuleBasedSegmentStorage = checkNotNull(ruleBasedSegmentStorage);
        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
        mMyLargeSegmentsStorage = checkNotNull(myLargeSegmentsStorage);
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

        if (isKeyExcluded(parsedRuleBasedSegment, matchingKey)) {
            return false;
        }

        if (inExcludedSegment(parsedRuleBasedSegment, matchingKey, bucketingKey, attributes, evaluator)) {
            return false;
        }

        return matchesConditions(bucketingKey, attributes, evaluator, parsedRuleBasedSegment, matchingKey);
    }

    private static boolean isKeyExcluded(ParsedRuleBasedSegment parsedRuleBasedSegment, String matchingKey) {
        return parsedRuleBasedSegment.getExcludedKeys().contains(matchingKey);
    }

    private boolean inExcludedSegment(ParsedRuleBasedSegment parsedRuleBasedSegment, Object matchingKey, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        for (ExcludedSegment segment : parsedRuleBasedSegment.getExcludedSegments()) {
            if (segment.isStandard() && mMySegmentsStorage.getAll().contains(segment.getName())) {
                return true;
            }

            if (segment.isRuleBased()) {
                InRuleBasedSegmentMatcher inRuleBasedSegmentMatcher = new InRuleBasedSegmentMatcher(mRuleBasedSegmentStorage, mMySegmentsStorage, mMyLargeSegmentsStorage, segment.getName());
                if (inRuleBasedSegmentMatcher.match(matchingKey, bucketingKey, attributes, evaluator)) {
                    return true;
                }
            }

            if (segment.isLarge() && mMyLargeSegmentsStorage.getAll().contains(segment.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesConditions(String bucketingKey, Map<String, Object> attributes, Evaluator evaluator, ParsedRuleBasedSegment parsedRuleBasedSegment, String matchingKey) {
        for (ParsedCondition condition : parsedRuleBasedSegment.getParsedConditions()) {
            if (condition.matcher().match(matchingKey, bucketingKey, attributes, evaluator)) {
                return true;
            }
        }
        return false;
    }
}
