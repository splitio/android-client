package io.split.android.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.api.Key;
import io.split.android.client.dtos.ConditionType;
import io.split.android.client.dtos.DataType;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.utils.SplitClientImplFactory;
import io.split.android.engine.experiments.ParsedCondition;
import io.split.android.engine.experiments.ParsedSplit;
import io.split.android.engine.experiments.SplitFetcher;
import io.split.android.engine.matchers.AllKeysMatcher;
import io.split.android.engine.matchers.CombiningMatcher;
import io.split.android.engine.matchers.DependencyMatcher;
import io.split.android.engine.matchers.EqualToMatcher;
import io.split.android.engine.matchers.GreaterThanOrEqualToMatcher;
import io.split.android.engine.matchers.collections.ContainsAnyOfSetMatcher;
import io.split.android.engine.matchers.strings.WhitelistMatcher;
import io.split.android.grammar.Treatments;

import static io.split.android.engine.ConditionsTestUtil.partition;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for SplitClientImpl
 */
public class SplitClientImplTest {


    @Test
    public void null_key_results_in_control() {
        String test = "test1";
        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("test1"), splitFetcher);

        assertThat(client.getTreatment(null), is(equalTo(Treatments.CONTROL)));

        verifyZeroInteractions(splitFetcher);
    }

    @Test
    public void null_test_results_in_control() {
        String test = "test1";
        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("adil@codigo.com"), splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(null, null), is(equalTo(Treatments.CONTROL)));

            }
        });
    }

    @Test
    public void exceptions_result_in_control() {
        SplitFetcher splitFetcher = mock(SplitFetcher.class);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("adil@relateiq.com"), splitFetcher);

        assertThat(client.getTreatment("test1"), is(equalTo(Treatments.CONTROL)));

    }

    @Test
    public void works() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("adil@codigo.com"), splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test), is(equalTo("on")));
            }
        });


    }


    @Test
    public void last_condition_is_always_default() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(
                CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))),
                Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, "user", 1, 1, null);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("pato@codigo.com"), splitFetcher);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test), is(equalTo(Treatments.OFF)));
            }
        });

    }

    @Test
    public void multiple_conditions_work() {
        String test = "test1";

        ParsedCondition adil_is_always_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        ParsedCondition pato_is_never_shown = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("pato@codigo.com"))), Lists.newArrayList(partition("off", 100)));
        ParsedCondition trevor_is_always_shown = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("trevor@codigo.com"))), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(adil_is_always_on, pato_is_never_shown, trevor_is_always_shown);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("adil@codigo.com"), splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test), is(equalTo("on")));
            }
        });

        client = SplitClientImplFactory.get(Key.withMatchingKey("pato@codigo.com"), splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {

                assertThat(client.getTreatment(test), is(equalTo("off")));
            }
        });

        client = SplitClientImplFactory.get(Key.withMatchingKey("trevor@codigo.com"), splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test), is(equalTo("on")));
            }
        });
    }


    @Test
    public void killed_test_always_goes_to_default() {
        String test = "test1";

        ParsedCondition rollOutToEveryone = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        List<ParsedCondition> conditions = Lists.newArrayList(rollOutToEveryone);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, true, Treatments.OFF, conditions, "user", 1, 1, null);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("adil@codigo.com"), splitFetcher);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test), is(equalTo(Treatments.OFF)));
            }
        });


    }

    @Test
    public void dependency_matcher_on() {
        String parent = "parent";
        String dependent = "dependent";

        ParsedCondition parent_is_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition(Treatments.ON, 100)));
        List<ParsedCondition> parent_conditions = Lists.newArrayList(parent_is_on);
        ParsedSplit parentSplit = ParsedSplit.createParsedSplitForTests(parent, 123, false, Treatments.OFF, parent_conditions, null, 1, 1, null);

        ParsedCondition dependent_needs_parent = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new DependencyMatcher(parent, Lists.newArrayList(Treatments.ON))), Lists.newArrayList(partition(Treatments.ON, 100)));
        List<ParsedCondition> dependent_conditions = Lists.newArrayList(dependent_needs_parent);
        ParsedSplit dependentSplit = ParsedSplit.createParsedSplitForTests(dependent, 123, false, Treatments.OFF, dependent_conditions, null, 1, 1, null);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(parent)).thenReturn(parentSplit);
        when(splitFetcher.fetch(dependent)).thenReturn(dependentSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("key"), splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(parent), is(equalTo(Treatments.ON)));
                assertThat(client.getTreatment(dependent), is(equalTo(Treatments.ON)));
            }
        });
    }

    @Test
    public void dependency_matcher_off() {
        String parent = "parent";
        String dependent = "dependent";

        ParsedCondition parent_is_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition(Treatments.ON, 100)));
        List<ParsedCondition> parent_conditions = Lists.newArrayList(parent_is_on);
        ParsedSplit parentSplit = ParsedSplit.createParsedSplitForTests(parent, 123, false, Treatments.OFF, parent_conditions, null, 1, 1, null);

        ParsedCondition dependent_needs_parent = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new DependencyMatcher(parent, Lists.newArrayList(Treatments.OFF))), Lists.newArrayList(partition(Treatments.ON, 100)));
        List<ParsedCondition> dependent_conditions = Lists.newArrayList(dependent_needs_parent);
        ParsedSplit dependentSplit = ParsedSplit.createParsedSplitForTests(dependent, 123, false, Treatments.OFF, dependent_conditions, null, 1, 1, null);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(parent)).thenReturn(parentSplit);
        when(splitFetcher.fetch(dependent)).thenReturn(dependentSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("key"), splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(parent), is(equalTo(Treatments.ON)));
                assertThat(client.getTreatment(dependent), is(equalTo(Treatments.OFF)));
            }
        });
    }

    @Test
    public void dependency_matcher_control() {
        String dependent = "dependent";

        ParsedCondition dependent_needs_parent = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new DependencyMatcher("not-exists", Lists.newArrayList(Treatments.OFF))), Lists.newArrayList(partition(Treatments.OFF, 100)));
        List<ParsedCondition> dependent_conditions = Lists.newArrayList(dependent_needs_parent);
        ParsedSplit dependentSplit = ParsedSplit.createParsedSplitForTests(dependent, 123, false, Treatments.ON, dependent_conditions, null, 1, 1, null);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(dependent)).thenReturn(dependentSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("key"), splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(dependent), is(equalTo(Treatments.ON)));
            }
        });

    }

    @Test
    public void attributes_work() {
        String test = "test1";

        ParsedCondition adil_is_always_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition(Treatments.ON, 100)));
        ParsedCondition users_with_age_greater_than_10_are_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of("age", new GreaterThanOrEqualToMatcher(10, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(adil_is_always_on, users_with_age_greater_than_10_are_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("adil@codigo.com"), splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test), is(equalTo("on")));
                assertThat(client.getTreatment(test, null), is(equalTo("on")));
                assertThat(client.getTreatment(test, ImmutableMap.<String, Object>of()), is(equalTo("on")));
            }
        });

        client = SplitClientImplFactory.get(Key.withMatchingKey("pato@codigo.com"), splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test, ImmutableMap.<String, Object>of("age", 10)), is(equalTo("on")));
                assertThat(client.getTreatment(test, ImmutableMap.<String, Object>of("age", 9)), is(equalTo("off")));

            }
        });
    }

    @Test
    public void attributes_work_2() {
        String test = "test1";

        ParsedCondition age_equal_to_0_should_be_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of("age", new EqualToMatcher(0, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, "user", 1, 1, null);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("adil@codigo.com"), splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test), is(equalTo("off")));
                assertThat(client.getTreatment(test, null), is(equalTo("off")));
                assertThat(client.getTreatment(test, ImmutableMap.<String, Object>of()), is(equalTo("off")));
            }
        });

        client = SplitClientImplFactory.get(Key.withMatchingKey("pato@codigo.com"), splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test, ImmutableMap.<String, Object>of("age", 10)), is(equalTo("off")));
                assertThat(client.getTreatment(test, ImmutableMap.<String, Object>of("age", 0)), is(equalTo("on")));
            }
        });
    }

    @Test
    public void attributes_greater_than_negative_number() {
        String test = "test1";

        ParsedCondition age_equal_to_0_should_be_on = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of("age", new EqualToMatcher(-20, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("adil@codigo.com"), splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test), is(equalTo("off")));
                assertThat(client.getTreatment(test, null), is(equalTo("off")));
                assertThat(client.getTreatment(test, ImmutableMap.of()), is(equalTo("off")));
            }
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        client = SplitClientImplFactory.get(Key.withMatchingKey("pato@codigo.com"), splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test, ImmutableMap.of("age", 10)), is(equalTo("off")));
                assertThat(client.getTreatment(test, ImmutableMap.of("age", -20)), is(equalTo("on")));
                assertThat(client.getTreatment(test, ImmutableMap.of("age", 20)), is(equalTo("off")));
                assertThat(client.getTreatment(test, ImmutableMap.of("age", -21)), is(equalTo("off")));
            }
        });
    }


    @Test
    public void attributes_for_sets() {
        String test = "test1";

        ParsedCondition any_of_set = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of("products", new ContainsAnyOfSetMatcher(Lists.<String>newArrayList("sms", "video"))), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(any_of_set);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("adil@codigo.com"), splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test), is(equalTo("off")));
                assertThat(client.getTreatment(test, null), is(equalTo("off")));
                assertThat(client.getTreatment(test, ImmutableMap.of()), is(equalTo("off")));
            }
        });

        client = SplitClientImplFactory.get(Key.withMatchingKey("pato@codigo.com"), splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test, ImmutableMap.of("products", Lists.newArrayList())), is(equalTo("off")));
                assertThat(client.getTreatment(test, ImmutableMap.of("products", Lists.newArrayList(""))), is(equalTo("off")));
                assertThat(client.getTreatment(test, ImmutableMap.of("products", Lists.newArrayList("talk"))), is(equalTo("off")));
                assertThat(client.getTreatment(test, ImmutableMap.of("products", Lists.newArrayList("sms"))), is(equalTo("on")));
                assertThat(client.getTreatment(test, ImmutableMap.of("products", Lists.newArrayList("sms", "video"))), is(equalTo("on")));
                assertThat(client.getTreatment(test, ImmutableMap.of("products", Lists.newArrayList("video"))), is(equalTo("on")));
            }
        });


    }

    @Test
    public void labels_are_populated() {
        String test = "test1";

        ParsedCondition age_equal_to_0_should_be_on = new ParsedCondition(ConditionType.ROLLOUT,
                CombiningMatcher.of("age", new EqualToMatcher(-20, DataType.NUMBER)),
                Lists.newArrayList(partition("on", 100)),
                "foolabel"
        );

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);
        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);
        ImpressionListener impressionListener = mock(ImpressionListener.class);
        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("pato@codigo.com"), splitFetcher, impressionListener);
        Map<String, Object> attributes = ImmutableMap.<String, Object>of("age", -20, "acv", "1000000");


        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test, attributes), is(equalTo("on")));
                ArgumentCaptor<Impression> impressionCaptor = ArgumentCaptor.forClass(Impression.class);
                verify(impressionListener).log(impressionCaptor.capture());
                assertThat(impressionCaptor.getValue().appliedRule(), is(equalTo("foolabel")));
                assertThat(impressionCaptor.getValue().attributes(), is(attributes));
            }
        });


    }

    @Test
    public void not_in_split_if_no_allocation() {
        traffic_allocation(Key.withMatchingKey("pato@split.io"), 0, 123, "off", "not in split");
    }

    /**
     * This test depends on the underlying hashing algorithm. I have
     * figured out that pato@split.io will be in bucket 10 for seed 123.
     * That is why the test has been set up this way.
     * <p>
     * If the underlying hashing algorithm changes, say to murmur, then we will
     * have to update this test.
     */
    @Test
    public void not_in_split_if_10_percent_allocation() {
        Key key = Key.withMatchingKey("pato@split.io");
        int i = 0;
        for (; i <= 9; i++) {
            traffic_allocation(key, i, 123, "off", "not in split");
        }

        for (; i <= 100; i++) {
            traffic_allocation(key, i, 123, "on", "in segment all");
        }
    }

    @Test
    public void in_split_if_100_percent_allocation() {
        traffic_allocation(Key.withMatchingKey("pato@split.io"), 100, 123, "on", "in segment all");
    }

    @Test
    public void in_split_if_1_percent_allocation() {
        traffic_allocation(Key.withMatchingKey("aaaaaaklmnbv"), 1, -1667452163, "on", "in segment all", 2);
    }

    @Test
    public void whitelist_overrides_traffic_allocation() {
        traffic_allocation(Key.withMatchingKey("adil@split.io"), 0, 123, "on", "whitelisted user");
    }

    private void traffic_allocation(Key key, int trafficAllocation, int trafficAllocationSeed, String expected_treatment_on_or_off, String label) {
        traffic_allocation(key, trafficAllocation, trafficAllocationSeed, expected_treatment_on_or_off, label, 1);
    }

    private void traffic_allocation(Key key, int trafficAllocation, int trafficAllocationSeed, String expected_treatment_on_or_off, String label, int algo) {

        String test = "test1";

        ParsedCondition whitelistCondition = new ParsedCondition(ConditionType.WHITELIST, CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@split.io"))), Lists.newArrayList(partition("on", 100), partition("off", 0)), "whitelisted user");
        ParsedCondition rollOutToEveryone = new ParsedCondition(ConditionType.ROLLOUT, CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100), partition("off", 0)), "in segment all");

        List<ParsedCondition> conditions = Lists.newArrayList(whitelistCondition, rollOutToEveryone);

        ParsedSplit parsedSplit = new ParsedSplit(test, 123, false, Treatments.OFF, conditions, null, 1, trafficAllocation, trafficAllocationSeed, algo, null);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        ImpressionListener impressionListener = mock(ImpressionListener.class);

        SplitClientImpl client = SplitClientImplFactory.get(key, splitFetcher, impressionListener);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test), is(equalTo(expected_treatment_on_or_off)));

                ArgumentCaptor<Impression> impressionCaptor = ArgumentCaptor.forClass(Impression.class);

                verify(impressionListener).log(impressionCaptor.capture());

                assertThat(impressionCaptor.getValue().appliedRule(), is(equalTo(label)));
            }
        });


    }


    @Test
    public void matching_bucketing_keys_work() {
        String test = "test1";


        Set<String> whitelist = new HashSet<>();
        whitelist.add("aijaz");
        ParsedCondition aijaz_should_match = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new WhitelistMatcher(whitelist)), Lists.newArrayList(partition("on", 100)));

        List<ParsedCondition> conditions = Lists.newArrayList(aijaz_should_match);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, "user", 1, 1, null);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);


        Key bad_key = new Key("adil", "aijaz");
        SplitClientImpl client = SplitClientImplFactory.get(bad_key, splitFetcher);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test, Collections.<String, Object>emptyMap()), is(equalTo("off")));
            }
        });

        Key good_key = new Key("aijaz", "adil");
        client = SplitClientImplFactory.get(good_key, splitFetcher);


        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test, Collections.<String, Object>emptyMap()), is(equalTo("on")));
            }
        });

    }

    @Test
    public void impression_metadata_is_propagated() {
        String test = "test1";

        ParsedCondition age_equal_to_0_should_be_on = new ParsedCondition(ConditionType.ROLLOUT,
                CombiningMatcher.of("age", new EqualToMatcher(-20, DataType.NUMBER)),
                Lists.newArrayList(partition("on", 100)),
                "foolabel"
        );

        List<ParsedCondition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        ParsedSplit parsedSplit = ParsedSplit.createParsedSplitForTests(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);

        SplitFetcher splitFetcher = mock(SplitFetcher.class);
        when(splitFetcher.fetch(test)).thenReturn(parsedSplit);

        ImpressionListener impressionListener = mock(ImpressionListener.class);


        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("pato@codigo.com"), splitFetcher, impressionListener);

        Map<String, Object> attributes = ImmutableMap.<String, Object>of("age", -20, "acv", "1000000");

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test, attributes), is(equalTo("on")));
                ArgumentCaptor<Impression> impressionCaptor = ArgumentCaptor.forClass(Impression.class);

                verify(impressionListener).log(impressionCaptor.capture());

                assertThat(impressionCaptor.getValue().appliedRule(), is(equalTo("foolabel")));
                assertThat(impressionCaptor.getValue().attributes(), is(equalTo(attributes)));
            }
        });
    }
}
