package io.split.android.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Condition;
import io.split.android.client.dtos.ConditionType;
import io.split.android.client.dtos.Matcher;
import io.split.android.client.dtos.MatcherCombiner;
import io.split.android.client.dtos.MatcherGroup;
import io.split.android.client.dtos.MatcherType;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.engine.experiments.ParsedCondition;
import io.split.android.engine.experiments.ParsedSplit;
import io.split.android.engine.matchers.CombiningMatcher;

public class SplitHelper {
    public static Map<String, String> createConfigs(List<String> treatments, List<String> configs) {
        Map<String, String> config = new HashMap<>();
        int i = 0;
        for (String treatment : treatments) {
            if (i > configs.size() - 1) {
                return config;
            }
            config.put(treatment, configs.get(i));
            i++;
        }
        return config;
    }

    public static Split createSplit(
            String feature,
            int seed,
            boolean killed,
            String defaultTreatment,
            List<Condition> conditions,
            String trafficTypeName,
            long changeNumber,
            int algo,
            Map<String, String> configurations
    ) {
        Split split = new Split();
        split.name = feature;
        split.seed = seed;
        split.killed = killed;
        split.defaultTreatment = defaultTreatment;
        split.conditions = conditions;
        split.trafficTypeName = trafficTypeName;
        split.changeNumber = changeNumber;
        split.trafficAllocation = 100;
        split.seed = seed;
        split.trafficAllocationSeed = seed;
        split.algo = algo;
        split.status = Status.ACTIVE;
        split.configurations = configurations;
        return split;
    }

    public static Condition createCondition(CombiningMatcher combiningMatcher, List<Partition> partitions) {
        Condition condition = new Condition();
        condition.conditionType = ConditionType.ROLLOUT;
        condition.matcherGroup = new MatcherGroup();
        condition.matcherGroup.combiner = MatcherCombiner.AND;
        condition.matcherGroup.matchers = new ArrayList<>();
        Matcher matcher = new Matcher();
        matcher.matcherType = MatcherType.ALL_KEYS;
        condition.matcherGroup.matchers.add(matcher);
        condition.partitions = partitions;
        return condition;
    }

    public static ParsedSplit createParsedSplit(
            String feature,
            int seed,
            boolean killed,
            String defaultTreatment,
            List<ParsedCondition> matcherAndSplits,
            String trafficTypeName,
            long changeNumber,
            int algo,
            Map<String, String> configurations
    ) {
        return new ParsedSplit(
                feature,
                seed,
                killed,
                defaultTreatment,
                matcherAndSplits,
                trafficTypeName,
                changeNumber,
                100,
                seed,
                algo,
                configurations
        );
    }

    public static ParsedCondition createParsedCondition(CombiningMatcher matcher, List<Partition> partitions) {
        return new ParsedCondition(ConditionType.ROLLOUT, matcher, partitions, null);
    }
}
