package io.split.android.engine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.split.android.client.dtos.BetweenMatcherData;
import io.split.android.client.dtos.Condition;
import io.split.android.client.dtos.ConditionType;
import io.split.android.client.dtos.DataType;
import io.split.android.client.dtos.KeySelector;
import io.split.android.client.dtos.Matcher;
import io.split.android.client.dtos.MatcherCombiner;
import io.split.android.client.dtos.MatcherGroup;
import io.split.android.client.dtos.MatcherType;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.UnaryNumericMatcherData;
import io.split.android.client.dtos.UserDefinedSegmentMatcherData;
import io.split.android.client.dtos.WhitelistMatcherData;

/**
 * Utility methods for creating conditions for testing purposes.
 *
 */
public class ConditionsTestUtil {

    public static Condition and(Matcher matcher1, List<Partition> partitions) {
        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Collections.singletonList(matcher1);

        Condition c = new Condition();
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    public static Condition and(Matcher matcher1, Matcher matcher2, List<Partition> partitions) {
        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Arrays.asList(matcher1, matcher2);

        Condition c = new Condition();
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    public static Condition makeWhitelistCondition(ConditionType conditionType, List<String> whitelist, List<Partition> partitions) {
        return makeWhitelistCondition(conditionType, whitelist, partitions, false);
    }

    private static Condition makeWhitelistCondition(ConditionType conditionType, List<String> whitelist, List<Partition> partitions, boolean negate) {
        Matcher matcher = whitelistMatcher(whitelist, negate);

        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Collections.singletonList(matcher);

        Condition c = new Condition();
        c.conditionType = conditionType;
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    private static Matcher whitelistMatcher(List<String> whitelist, boolean negate) {
        return whitelistMatcher(null, null, whitelist, negate, MatcherType.WHITELIST);
    }

    private static Matcher whitelistMatcher(String trafficType, String attribute, List<String> whitelist, boolean negate, MatcherType matcherType) {
        WhitelistMatcherData whitelistMatcherData = new WhitelistMatcherData();
        whitelistMatcherData.whitelist = whitelist;

        KeySelector keySelector = null;
        if (trafficType != null && attribute != null) {
            keySelector = new KeySelector();
            keySelector.trafficType = trafficType;
            keySelector.attribute = attribute;
        }
        Matcher matcher = new Matcher();
        matcher.keySelector = keySelector;
        matcher.matcherType = matcherType;
        matcher.negate = negate;
        matcher.whitelistMatcherData = whitelistMatcherData;
        return matcher;
    }

    public static Condition makeUserDefinedSegmentCondition(ConditionType conditionType, String segment, List<Partition> partitions) {
        return makeUserDefinedSegmentCondition(conditionType, segment, partitions, false);
    }

    private static Condition makeUserDefinedSegmentCondition(ConditionType conditionType, String segment, List<Partition> partitions, boolean negate) {
        Matcher matcher = userDefinedSegmentMatcher(segment, negate);

        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Collections.singletonList(matcher);

        Condition c = new Condition();
        c.conditionType = conditionType;
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    private static Matcher userDefinedSegmentMatcher(String segment, boolean negate) {
        return userDefinedSegmentMatcher(null, null, segment, negate);
    }

    private static Matcher userDefinedSegmentMatcher(String trafficType, String attribute, String segment, boolean negate) {
        UserDefinedSegmentMatcherData userDefinedSegment = new UserDefinedSegmentMatcherData();
        userDefinedSegment.segmentName = segment;

        KeySelector keySelector = null;
        if (trafficType != null && attribute != null) {
            keySelector = new KeySelector();
            keySelector.trafficType = trafficType;
            keySelector.attribute = attribute;
        }

        Matcher matcher = new Matcher();
        matcher.keySelector = keySelector;
        matcher.matcherType = MatcherType.IN_SEGMENT;
        matcher.negate = negate;
        matcher.userDefinedSegmentMatcherData = userDefinedSegment;
        return matcher;
    }

    public static Condition makeAllKeysCondition(List<Partition> partitions) {
        return makeAllKeysCondition(partitions, false);
    }

    private static Condition makeAllKeysCondition(List<Partition> partitions, boolean negate) {
        Matcher matcher = allKeysMatcher(negate);

        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Collections.singletonList(matcher);

        Condition c = new Condition();
        c.conditionType = ConditionType.ROLLOUT;
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    private static Matcher allKeysMatcher(boolean negate) {
        Matcher matcher = new Matcher();
        matcher.matcherType = MatcherType.ALL_KEYS;
        matcher.negate = negate;
        return matcher;
    }

    public static Matcher numericMatcher(String trafficType, String attribute,
                                         MatcherType matcherType, DataType dataType,
                                         long number, boolean negate) {

        KeySelector keySelector = new KeySelector();
        keySelector.trafficType = trafficType;
        keySelector.attribute = attribute;

        UnaryNumericMatcherData numericMatcherData = new UnaryNumericMatcherData();
        numericMatcherData.dataType = dataType;
        numericMatcherData.value = number;

        Matcher matcher = new Matcher();
        matcher.keySelector = keySelector;
        matcher.matcherType = matcherType;
        matcher.negate = negate;
        matcher.unaryNumericMatcherData = numericMatcherData;

        return matcher;
    }

    public static Matcher betweenMatcher(String trafficType,
                                         String attribute,
                                         DataType dataType,
                                         long start,
                                         long end,
                                         boolean negate) {

        KeySelector keySelector = new KeySelector();
        keySelector.trafficType = trafficType;
        keySelector.attribute = attribute;

        BetweenMatcherData betweenMatcherData = new BetweenMatcherData();
        betweenMatcherData.dataType = dataType;
        betweenMatcherData.start = start;
        betweenMatcherData.end = end;

        Matcher matcher = new Matcher();
        matcher.keySelector = keySelector;
        matcher.matcherType = MatcherType.BETWEEN;
        matcher.negate = negate;
        matcher.betweenMatcherData = betweenMatcherData;

        return matcher;
    }

    public static Condition containsAnyOfSet(String trafficType,
                                         String attribute,
                                         List<String> whitelist,
                                         boolean negate,
                                         List<Partition> partitions) {

        Matcher matcher = whitelistMatcher(trafficType, attribute, whitelist, negate, MatcherType.CONTAINS_ANY_OF_SET);
        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Collections.singletonList(matcher);

        Condition c = new Condition();
        c.conditionType = ConditionType.ROLLOUT;
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    public static Condition containsAllOfSet(String trafficType,
                                           String attribute,
                                           List<String> whitelist,
                                           boolean negate,
                                           List<Partition> partitions) {

        Matcher matcher = whitelistMatcher(trafficType, attribute, whitelist, negate, MatcherType.CONTAINS_ALL_OF_SET);
        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Collections.singletonList(matcher);

        Condition c = new Condition();
        c.conditionType = ConditionType.ROLLOUT;
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    public static Condition isPartOfSet(String trafficType,
                                           String attribute,
                                           List<String> whitelist,
                                           boolean negate,
                                           List<Partition> partitions) {

        Matcher matcher = whitelistMatcher(trafficType, attribute, whitelist, negate, MatcherType.PART_OF_SET);
        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Collections.singletonList(matcher);

        Condition c = new Condition();
        c.conditionType = ConditionType.ROLLOUT;
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    public static Condition startsWithString(String trafficType,
                                        String attribute,
                                        List<String> whitelist,
                                        boolean negate,
                                        List<Partition> partitions) {

        Matcher matcher = whitelistMatcher(trafficType, attribute, whitelist, negate, MatcherType.STARTS_WITH);
        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Collections.singletonList(matcher);

        Condition c = new Condition();
        c.conditionType = ConditionType.ROLLOUT;
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    public static Condition endsWithString(String trafficType,
                                             String attribute,
                                             List<String> whitelist,
                                             boolean negate,
                                             List<Partition> partitions) {

        Matcher matcher = whitelistMatcher(trafficType, attribute, whitelist, negate, MatcherType.ENDS_WITH);
        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Collections.singletonList(matcher);

        Condition c = new Condition();
        c.conditionType = ConditionType.ROLLOUT;
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    public static Condition containsString(String trafficType,
                                             String attribute,
                                             List<String> whitelist,
                                             boolean negate,
                                             List<Partition> partitions) {

        Matcher matcher = whitelistMatcher(trafficType, attribute, whitelist, negate, MatcherType.CONTAINS_STRING);
        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Collections.singletonList(matcher);

        Condition c = new Condition();
        c.conditionType = ConditionType.ROLLOUT;
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    public static Condition equalToSet(String trafficType,
                                           String attribute,
                                           List<String> whitelist,
                                           boolean negate,
                                           List<Partition> partitions) {

        Matcher matcher = whitelistMatcher(trafficType, attribute, whitelist, negate, MatcherType.EQUAL_TO_SET);
        MatcherGroup matcherGroup = new MatcherGroup();
        matcherGroup.combiner = MatcherCombiner.AND;
        matcherGroup.matchers = Collections.singletonList(matcher);

        Condition c = new Condition();
        c.conditionType = ConditionType.ROLLOUT;
        c.matcherGroup = matcherGroup;
        c.partitions = partitions;

        return c;
    }

    public static Partition partition(String treatment, int size) {
        Partition p = new Partition();
        p.treatment = treatment;
        p.size = size;
        return p;
    }
}
