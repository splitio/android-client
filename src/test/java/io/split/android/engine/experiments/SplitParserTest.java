package io.split.android.engine.experiments;

import com.google.common.collect.Lists;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Condition;
import io.split.android.client.dtos.DataType;
import io.split.android.client.dtos.Matcher;
import io.split.android.client.dtos.MatcherCombiner;
import io.split.android.client.dtos.MatcherType;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.engine.ConditionsTestUtil;
import io.split.android.engine.matchers.AttributeMatcher;
import io.split.android.engine.matchers.BetweenMatcher;
import io.split.android.engine.matchers.CombiningMatcher;
import io.split.android.engine.matchers.EqualToMatcher;
import io.split.android.engine.matchers.LessThanOrEqualToMatcher;
import io.split.android.engine.matchers.collections.ContainsAllOfSetMatcher;
import io.split.android.engine.matchers.collections.ContainsAnyOfSetMatcher;
import io.split.android.engine.matchers.collections.EqualToSetMatcher;
import io.split.android.engine.matchers.collections.PartOfSetMatcher;
import io.split.android.engine.matchers.strings.ContainsAnyOfMatcher;
import io.split.android.engine.matchers.strings.EndsWithAnyOfMatcher;
import io.split.android.engine.matchers.strings.StartsWithAnyOfMatcher;
import io.split.android.engine.segments.RefreshableMySegmentsFetcherProviderImpl;
import io.split.android.engine.segments.StaticMySegmentsFectherProvider;
import io.split.android.grammar.Treatments;
import io.split.android.helpers.SplitHelper;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for ExperimentParser
 *
 */
public class SplitParserTest {

    @Test
    public void less_than_or_equal_to() {
        RefreshableMySegmentsFetcherProviderImpl provider = StaticMySegmentsFectherProvider.get("key");
        SplitParser parser = SplitParser.get(provider);

        Matcher ageLessThan10 = ConditionsTestUtil.numericMatcher("user", "age", MatcherType.LESS_THAN_OR_EQUAL_TO, DataType.NUMBER, 10L, false);

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.and(ageLessThan10, partitions);


        List<Condition> conditions = Lists.newArrayList(c);

        Map<String, String> configs = SplitHelper.createConfigs(Arrays.asList("t1","t2"), Arrays.asList("{\"f1\":\"v1\"}", "{\"f2\":\"v2\"}"));

        Split split = makeSplit("first.name", 123, conditions, 1, configs);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher ageLessThan10Logic = new AttributeMatcher("age", new LessThanOrEqualToMatcher(10, DataType.NUMBER), false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(ageLessThan10Logic));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, configs);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void equal_to() {

        RefreshableMySegmentsFetcherProviderImpl provider = StaticMySegmentsFectherProvider.get("key");

        SplitParser parser = SplitParser.get(provider);

        Matcher ageLessThan10 = ConditionsTestUtil.numericMatcher("user", "age", MatcherType.EQUAL_TO, DataType.NUMBER, 10L, true);

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.and(ageLessThan10, partitions);

        List<Condition> conditions = Lists.newArrayList(c);

        Split split = makeSplit("first.name", 123, conditions, 1);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher equalToMatcher = new AttributeMatcher("age", new EqualToMatcher(10, DataType.NUMBER), true);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(equalToMatcher));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, null);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void equal_to_negative_number() {

        SplitParser parser = new SplitParser(StaticMySegmentsFectherProvider.get("key"));

        Matcher equalToNegative10 = ConditionsTestUtil.numericMatcher("user", "age", MatcherType.EQUAL_TO, DataType.NUMBER, -10L, false);

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.and(equalToNegative10, partitions);

        List<Condition> conditions = Lists.newArrayList(c);

        Split split = makeSplit("first.name", 123, conditions, 1);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher ageEqualTo10Logic = new AttributeMatcher("age", new EqualToMatcher(-10, DataType.NUMBER), false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(ageEqualTo10Logic));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, null);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void between() {

        SplitParser parser = new SplitParser(StaticMySegmentsFectherProvider.get("key"));

        Matcher ageBetween10And11 = ConditionsTestUtil.betweenMatcher("user",
                "age",
                DataType.NUMBER,
                10,
                12,
                false);

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.and(ageBetween10And11, partitions);

        List<Condition> conditions = Lists.newArrayList(c);

        Split split = makeSplit("first.name", 123, conditions, 1);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher ageBetween10And11Logic = new AttributeMatcher("age", new BetweenMatcher(10, 12, DataType.NUMBER), false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(ageBetween10And11Logic));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, null);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void contains_any_of_set() {

        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.containsAnyOfSet("user",
                "products",
                set,
                false,
                partitions
                );

        ContainsAnyOfSetMatcher m = new ContainsAnyOfSetMatcher(set);

        set_matcher_test(c, m);
    }

    @Test
    public void contains_all_of_set() {

        ArrayList<String> set = Lists.newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.containsAllOfSet("user",
                "products",
                set,
                false,
                partitions
        );

        ContainsAllOfSetMatcher m = new ContainsAllOfSetMatcher(set);

        set_matcher_test(c, m);
    }

    @Test
    public void equal_to_set() {

        ArrayList<String> set = Lists.newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.equalToSet("user",
                "products",
                set,
                false,
                partitions
        );

        EqualToSetMatcher m = new EqualToSetMatcher(set);

        set_matcher_test(c, m);
    }

    @Test
    public void is_part_of_set() {

        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.isPartOfSet("user",
                "products",
                set,
                false,
                partitions
        );

        PartOfSetMatcher m = new PartOfSetMatcher(set);

        set_matcher_test(c, m);
    }

    @Test
    public void starts_with_string() {

        ArrayList<String> set = Lists.newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.startsWithString("user",
                "products",
                set,
                false,
                partitions
        );

        StartsWithAnyOfMatcher m = new StartsWithAnyOfMatcher(set);

        set_matcher_test(c, m);
    }

    @Test
    public void ends_with_string() {

        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.endsWithString("user",
                "products",
                set,
                false,
                partitions
        );

        EndsWithAnyOfMatcher m = new EndsWithAnyOfMatcher(set);

        set_matcher_test(c, m);
    }


    @Test
    public void contains_string() {

        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.containsString("user",
                "products",
                set,
                false,
                partitions
        );

        ContainsAnyOfMatcher m = new ContainsAnyOfMatcher(set);

        set_matcher_test(c, m);
    }

    public void set_matcher_test(Condition c, io.split.android.engine.matchers.Matcher m) {

        SplitParser parser = new SplitParser(StaticMySegmentsFectherProvider.get("key"));

        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));


        List<Condition> conditions = Lists.newArrayList(c);

        Split split = makeSplit("splitName", 123, conditions, 1);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher attrMatcher = new AttributeMatcher("products", m, false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(attrMatcher));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("splitName", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, null);

        assertThat(actual, is(equalTo(expected)));
    }

    private Split makeSplit(String name, int seed, List<Condition> conditions, long changeNumber) {
        return makeSplit(name, seed, conditions, changeNumber,null);
    }

    private Split makeSplit(String name, int seed, List<Condition> conditions, long changeNumber, Map<String, String> configurations) {
        Split split = new Split();
        split.name = name;
        split.seed = seed;
        split.trafficAllocation = 100;
        split.trafficAllocationSeed = seed;
        split.status = Status.ACTIVE;
        split.conditions = conditions;
        split.defaultTreatment = Treatments.OFF;
        split.trafficTypeName = "user";
        split.changeNumber = changeNumber;
        split.algo = 1;
        split.configurations = configurations;
        return split;
    }

}
