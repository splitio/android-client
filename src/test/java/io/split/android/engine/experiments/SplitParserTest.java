package io.split.android.engine.experiments;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.BetweenStringMatcherData;
import io.split.android.client.dtos.Condition;
import io.split.android.client.dtos.ConditionType;
import io.split.android.client.dtos.DataType;
import io.split.android.client.dtos.Matcher;
import io.split.android.client.dtos.MatcherCombiner;
import io.split.android.client.dtos.MatcherGroup;
import io.split.android.client.dtos.MatcherType;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
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
import io.split.android.grammar.Treatments;
import io.split.android.helpers.SplitHelper;

/**
 * Tests for ExperimentParser
 *
 */
public class SplitParserTest {

    @Mock
    MySegmentsStorage mMySegmentsStorage;
    @Mock
    MySegmentsStorageContainer mMySegmentsStorageContainer;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(mMySegmentsStorageContainer.getStorageForKey("")).thenReturn(mMySegmentsStorage);
    }

    @Test
    public void less_than_or_equal_to() {
        SplitParser parser = SplitParser.get(mMySegmentsStorageContainer);

        Matcher ageLessThan10 = ConditionsTestUtil.numericMatcher("user", "age", MatcherType.LESS_THAN_OR_EQUAL_TO, DataType.NUMBER, 10L, false);

        List<Partition> partitions = Arrays.asList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.and(ageLessThan10, partitions);


        List<Condition> conditions = Arrays.asList(c);

        Map<String, String> configs = SplitHelper.createConfigs(Arrays.asList("t1","t2"), Arrays.asList("{\"f1\":\"v1\"}", "{\"f2\":\"v2\"}"));

        Split split = makeSplit("first.name", conditions, 1, configs);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher ageLessThan10Logic = new AttributeMatcher("age", new LessThanOrEqualToMatcher(10, DataType.NUMBER), false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Arrays.asList(ageLessThan10Logic));
        ParsedCondition parsedCondition = SplitHelper.createParsedCondition(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Arrays.asList(parsedCondition);

        ParsedSplit expected = SplitHelper.createParsedSplit("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, configs);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void equal_to() {

        SplitParser parser = SplitParser.get(mMySegmentsStorageContainer);

        Matcher ageLessThan10 = ConditionsTestUtil.numericMatcher("user", "age", MatcherType.EQUAL_TO, DataType.NUMBER, 10L, true);

        List<Partition> partitions = Arrays.asList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.and(ageLessThan10, partitions);

        List<Condition> conditions = Arrays.asList(c);

        Split split = makeSplit("first.name", conditions);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher equalToMatcher = new AttributeMatcher("age", new EqualToMatcher(10, DataType.NUMBER), true);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Arrays.asList(equalToMatcher));
        ParsedCondition parsedCondition = SplitHelper.createParsedCondition(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Arrays.asList(parsedCondition);

        ParsedSplit expected = SplitHelper.createParsedSplit("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, null);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void equal_to_negative_number() {

        SplitParser parser = new SplitParser(mMySegmentsStorageContainer);

        Matcher equalToNegative10 = ConditionsTestUtil.numericMatcher("user", "age", MatcherType.EQUAL_TO, DataType.NUMBER, -10L, false);

        List<Partition> partitions = Arrays.asList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.and(equalToNegative10, partitions);

        List<Condition> conditions = Arrays.asList(c);

        Split split = makeSplit("first.name", conditions);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher ageEqualTo10Logic = new AttributeMatcher("age", new EqualToMatcher(-10, DataType.NUMBER), false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Arrays.asList(ageEqualTo10Logic));
        ParsedCondition parsedCondition = SplitHelper.createParsedCondition(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Arrays.asList(parsedCondition);

        ParsedSplit expected = SplitHelper.createParsedSplit("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, null);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void between() {

        SplitParser parser = new SplitParser(mMySegmentsStorageContainer);

        Matcher ageBetween10And11 = ConditionsTestUtil.betweenMatcher("user",
                "age",
                DataType.NUMBER,
                10,
                12,
                false);

        List<Partition> partitions = Arrays.asList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.and(ageBetween10And11, partitions);

        List<Condition> conditions = Arrays.asList(c);

        Split split = makeSplit("first.name", conditions);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher ageBetween10And11Logic = new AttributeMatcher("age", new BetweenMatcher(10, 12, DataType.NUMBER), false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Arrays.asList(ageBetween10And11Logic));
        ParsedCondition parsedCondition = SplitHelper.createParsedCondition(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Arrays.asList(parsedCondition);

        ParsedSplit expected = SplitHelper.createParsedSplit("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, null);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void contains_any_of_set() {

        List<String> set = Arrays.asList("sms", "voice");

        List<Partition> partitions = Arrays.asList(ConditionsTestUtil.partition("on", 100));

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

        List<String> set = Arrays.asList("sms", "voice");

        List<Partition> partitions = Arrays.asList(ConditionsTestUtil.partition("on", 100));

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

        List<String> set = Arrays.asList("sms", "voice");

        List<Partition> partitions = Arrays.asList(ConditionsTestUtil.partition("on", 100));

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

        List<String> set = Arrays.asList("sms", "voice");

        List<Partition> partitions = Arrays.asList(ConditionsTestUtil.partition("on", 100));

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

        List<String> set = Arrays.asList("sms", "voice");

        List<Partition> partitions = Arrays.asList(ConditionsTestUtil.partition("on", 100));

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

        List<String> set = Arrays.asList("sms", "voice");

        List<Partition> partitions = Arrays.asList(ConditionsTestUtil.partition("on", 100));

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

        List<String> set = Arrays.asList("sms", "voice");

        List<Partition> partitions = Arrays.asList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.containsString("user",
                "products",
                set,
                false,
                partitions
        );

        ContainsAnyOfMatcher m = new ContainsAnyOfMatcher(set);

        set_matcher_test(c, m);
    }

    @Test
    public void equalToSemverParsing() {
        Condition condition = new Condition();
        condition.conditionType = ConditionType.ROLLOUT;
        condition.label = "new label";
        condition.partitions = Arrays.asList(
                ConditionsTestUtil.partition("on", 50),
                ConditionsTestUtil.partition("0ff", 50));
        Matcher matcher = new Matcher();
        matcher.matcherType = MatcherType.EQUAL_TO_SEMVER;
        matcher.stringMatcherData = "2.2.2";
        condition.matcherGroup = new MatcherGroup();
        condition.matcherGroup.matchers = Collections.singletonList(matcher);
        Split split = makeSplit("test1", Collections.singletonList(condition));

        SplitParser parser = new SplitParser(mMySegmentsStorageContainer);

        ParsedSplit parsedSplit = parser.parse(split);
        assertEquals("test1", parsedSplit.feature());
        assertEquals("off", parsedSplit.defaultTreatment());
        assertEquals(1, parsedSplit.parsedConditions().size());
        ParsedCondition parsedCondition = parsedSplit.parsedConditions().get(0);
        assertEquals("new label", parsedCondition.label());
        assertEquals(ConditionType.ROLLOUT, parsedCondition.conditionType());
        assertEquals(2, parsedCondition.partitions().size());
    }

    @Test
    public void greaterThanOrEqualToSemverParsing() {
        Condition condition = new Condition();
        condition.conditionType = ConditionType.ROLLOUT;
        condition.label = "new label";
        condition.partitions = Arrays.asList(
                ConditionsTestUtil.partition("on", 50),
                ConditionsTestUtil.partition("0ff", 50));
        Matcher matcher = new Matcher();
        matcher.matcherType = MatcherType.GREATER_THAN_OR_EQUAL_TO_SEMVER;
        matcher.stringMatcherData = "2.2.2";
        condition.matcherGroup = new MatcherGroup();
        condition.matcherGroup.matchers = Collections.singletonList(matcher);
        Split split = makeSplit("test1", Collections.singletonList(condition));

        SplitParser parser = new SplitParser(mMySegmentsStorageContainer);

        ParsedSplit parsedSplit = parser.parse(split);
        assertEquals("test1", parsedSplit.feature());
        assertEquals("off", parsedSplit.defaultTreatment());
        assertEquals(1, parsedSplit.parsedConditions().size());
        ParsedCondition parsedCondition = parsedSplit.parsedConditions().get(0);
        assertEquals("new label", parsedCondition.label());
        assertEquals(ConditionType.ROLLOUT, parsedCondition.conditionType());
        assertEquals(2, parsedCondition.partitions().size());
    }

    @Test
    public void lessThanOrEqualToSemverParsing() {
        Condition condition = new Condition();
        condition.conditionType = ConditionType.ROLLOUT;
        condition.label = "new label";
        condition.partitions = Arrays.asList(
                ConditionsTestUtil.partition("on", 50),
                ConditionsTestUtil.partition("0ff", 50));
        Matcher matcher = new Matcher();
        matcher.matcherType = MatcherType.LESS_THAN_OR_EQUAL_TO_SEMVER;
        matcher.stringMatcherData = "2.2.2";
        condition.matcherGroup = new MatcherGroup();
        condition.matcherGroup.matchers = Collections.singletonList(matcher);
        Split split = makeSplit("test1", Collections.singletonList(condition));

        SplitParser parser = new SplitParser(mMySegmentsStorageContainer);

        ParsedSplit parsedSplit = parser.parse(split);
        assertEquals("test1", parsedSplit.feature());
        assertEquals("off", parsedSplit.defaultTreatment());
        assertEquals(1, parsedSplit.parsedConditions().size());
        ParsedCondition parsedCondition = parsedSplit.parsedConditions().get(0);
        assertEquals("new label", parsedCondition.label());
        assertEquals(ConditionType.ROLLOUT, parsedCondition.conditionType());
        assertEquals(2, parsedCondition.partitions().size());
    }

    @Test
    public void betweenSemverParsing() {
        Condition condition = new Condition();
        condition.conditionType = ConditionType.ROLLOUT;
        condition.label = "new label";
        condition.partitions = Arrays.asList(
                ConditionsTestUtil.partition("on", 50),
                ConditionsTestUtil.partition("0ff", 50));
        Matcher matcher = new Matcher();
        matcher.matcherType = MatcherType.BETWEEN_SEMVER;
        BetweenStringMatcherData betweenStringMatcherData = new BetweenStringMatcherData();
        betweenStringMatcherData.start = "2.2.2";
        betweenStringMatcherData.end = "2.2.3";
        matcher.betweenStringMatcherData = betweenStringMatcherData;
        condition.matcherGroup = new MatcherGroup();
        condition.matcherGroup.matchers = Collections.singletonList(matcher);
        Split split = makeSplit("test1", Collections.singletonList(condition));

        SplitParser parser = new SplitParser(mMySegmentsStorageContainer);

        ParsedSplit parsedSplit = parser.parse(split);
        assertEquals("test1", parsedSplit.feature());
        assertEquals("off", parsedSplit.defaultTreatment());
        assertEquals(1, parsedSplit.parsedConditions().size());
        ParsedCondition parsedCondition = parsedSplit.parsedConditions().get(0);
        assertEquals("new label", parsedCondition.label());
        assertEquals(ConditionType.ROLLOUT, parsedCondition.conditionType());
        assertEquals(2, parsedCondition.partitions().size());
    }

    private void set_matcher_test(Condition c, io.split.android.engine.matchers.Matcher m) {

        SplitParser parser = new SplitParser(mMySegmentsStorageContainer);

        List<Partition> partitions = Arrays.asList(ConditionsTestUtil.partition("on", 100));


        List<Condition> conditions = Arrays.asList(c);

        Split split = makeSplit("splitName", conditions);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher attrMatcher = new AttributeMatcher("products", m, false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Arrays.asList(attrMatcher));
        ParsedCondition parsedCondition = SplitHelper.createParsedCondition(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Arrays.asList(parsedCondition);

        ParsedSplit expected = SplitHelper.createParsedSplit("splitName", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, null);

        assertThat(actual, is(equalTo(expected)));
    }

    private Split makeSplit(String name, List<Condition> conditions) {
        return makeSplit(name, conditions, (long) 1,null);
    }

    private Split makeSplit(String name, List<Condition> conditions, long changeNumber, Map<String, String> configurations) {
        Split split = new Split();
        split.name = name;
        split.seed = 123;
        split.trafficAllocation = 100;
        split.trafficAllocationSeed = 123;
        split.status = Status.ACTIVE;
        split.conditions = conditions;
        split.defaultTreatment = Treatments.OFF;
        split.trafficTypeName = "user";
        split.changeNumber = changeNumber;
        split.algo = 1;
        split.configurations = configurations;
        split.sets = Collections.emptySet();
        return split;
    }

}
