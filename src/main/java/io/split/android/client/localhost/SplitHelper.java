package io.split.android.client.localhost;

import java.util.Arrays;
import java.util.List;

import io.split.android.client.dtos.Algorithm;
import io.split.android.client.dtos.Condition;
import io.split.android.client.dtos.ConditionType;
import io.split.android.client.dtos.Matcher;
import io.split.android.client.dtos.MatcherCombiner;
import io.split.android.client.dtos.MatcherGroup;
import io.split.android.client.dtos.MatcherType;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.dtos.WhitelistMatcherData;
import io.split.android.grammar.Treatments;

class SplitHelper {

    static Split createDefaultSplit(String splitName) {
        Split split = new Split();
        split.name = splitName;
        split.defaultTreatment = Treatments.CONTROL;
        split.status = Status.ACTIVE;
        split.algo = Algorithm.MURMUR3;
        split.trafficTypeName = "custom";
        split.trafficAllocation = 100;
        split.trafficAllocationSeed = 1;
        split.seed = 1;
        return split;
    }

    static Condition createWhiteListCondition(List<String> keys, String treatment) {
        Condition condition = new Condition();
        MatcherGroup matcherGroup = new MatcherGroup();
        Matcher matcher = new Matcher();
        WhitelistMatcherData whitelistMatcherData = new WhitelistMatcherData();
        Partition partition = new Partition();

        condition.conditionType = ConditionType.WHITELIST;
        matcherGroup.combiner = MatcherCombiner.AND;
        matcher.matcherType = MatcherType.WHITELIST;
        whitelistMatcherData.whitelist = keys;
        matcher.whitelistMatcherData = whitelistMatcherData;
        partition.size = 100;
        partition.treatment = treatment;
        matcherGroup.matchers = Arrays.asList(matcher);
        condition.matcherGroup = matcherGroup;
        condition.partitions = Arrays.asList(partition);
        condition.label = "LOCAL_"+ keys.toString();
        return condition;
    }

    static Condition createRolloutCondition(String treatment) {
        Condition condition = new Condition();
        MatcherGroup matcherGroup = new MatcherGroup();
        Matcher matcher = new Matcher();
        Partition partition = new Partition();

        condition.conditionType = ConditionType.ROLLOUT;
        matcherGroup.combiner = MatcherCombiner.AND;
        matcher.matcherType = MatcherType.ALL_KEYS;
        partition.size = 100;
        partition.treatment = treatment;
        matcherGroup.matchers = Arrays.asList(matcher);
        condition.matcherGroup = matcherGroup;
        condition.partitions = Arrays.asList(partition);
        condition.label = "in segment all";
        return condition;

    }
}
