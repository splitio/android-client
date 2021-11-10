package io.split.android.client;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.split.android.engine.ConditionsTestUtil.partition;

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
import io.split.android.client.dtos.Condition;
import io.split.android.client.dtos.ConditionType;
import io.split.android.client.dtos.DataType;
import io.split.android.client.dtos.Split;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.SplitClientImplFactory;
import io.split.android.engine.matchers.AllKeysMatcher;
import io.split.android.engine.matchers.CombiningMatcher;
import io.split.android.engine.matchers.DependencyMatcher;
import io.split.android.engine.matchers.EqualToMatcher;
import io.split.android.engine.matchers.GreaterThanOrEqualToMatcher;
import io.split.android.engine.matchers.collections.ContainsAnyOfSetMatcher;
import io.split.android.engine.matchers.strings.WhitelistMatcher;
import io.split.android.grammar.Treatments;
import io.split.android.helpers.SplitHelper;

/**
 * Tests for SplitClientImpl
 */
public class SplitClientImplTest {

    SplitClient mClient;

    @Test
    public void null_key_results_in_control() {
        String test = "test1";
        Condition rollOutToEveryone = SplitHelper.createCondition(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition("on", 100)));
        List<Condition> conditions = Lists.newArrayList(rollOutToEveryone);
        Split parsedSplit = SplitHelper.createSplit(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);

        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(new Key("test1"), splitsStorage);

        assertThat(client.getTreatment(null), is(equalTo(Treatments.CONTROL)));

        verify(splitsStorage, never()).get(anyString());
    }

    @Test
    public void null_test_results_in_control() {
        String test = "test1";
        Condition rollOutToEveryone = SplitHelper.createCondition(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<Condition> conditions = Lists.newArrayList(rollOutToEveryone);
        Split parsedSplit = SplitHelper.createSplit(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);

        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(new Key("adil@codigo.com"), splitsStorage);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(null, null), is(equalTo(Treatments.CONTROL)));

            }
        });
    }

    @Test
    public void exceptions_result_in_control() {
        SplitsStorage splitsStorage = mock(SplitsStorage.class);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("adil@relateiq.com"), splitsStorage);

        assertThat(client.getTreatment("test1"), is(equalTo(Treatments.CONTROL)));

    }

    @Test
    public void works() {
        String test = "test1";

        Condition rollOutToEveryone = SplitHelper.createCondition(CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100)));
        List<Condition> conditions = Lists.newArrayList(rollOutToEveryone);
        Split parsedSplit = SplitHelper.createSplit(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);

        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("adil@codigo.com"), splitsStorage);

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

        Condition rollOutToEveryone = SplitHelper.createCondition(
                CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))),
                Lists.newArrayList(partition("on", 100)));
        List<Condition> conditions = Lists.newArrayList(rollOutToEveryone);
        Split parsedSplit = SplitHelper.createSplit(test, 123, false, Treatments.OFF, conditions, "user", 1, 1, null);

        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("pato@codigo.com"), splitsStorage);

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

        Condition adil_is_always_on = SplitHelper.createCondition(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        Condition pato_is_never_shown = SplitHelper.createCondition(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("pato@codigo.com"))), Lists.newArrayList(partition("off", 100)));
        Condition trevor_is_always_shown = SplitHelper.createCondition(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("trevor@codigo.com"))), Lists.newArrayList(partition("on", 100)));

        List<Condition> conditions = Lists.newArrayList(adil_is_always_on, pato_is_never_shown, trevor_is_always_shown);
        Split parsedSplit = SplitHelper.createSplit(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);

        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(test)).thenReturn(parsedSplit);

        mClient = SplitClientImplFactory.get(Key.withMatchingKey("adil@codigo.com"), splitsStorage);

        mClient.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test), is(equalTo("on")));
            }
        });

        mClient = SplitClientImplFactory.get(Key.withMatchingKey("pato@codigo.com"), splitsStorage);

        mClient.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {

                assertThat(client.getTreatment(test), is(equalTo("off")));
            }
        });

        mClient = SplitClientImplFactory.get(Key.withMatchingKey("trevor@codigo.com"), splitsStorage);

        mClient.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test), is(equalTo("on")));
            }
        });
    }


    @Test
    public void killed_test_always_goes_to_default() {
        String test = "test1";

        Condition rollOutToEveryone = SplitHelper.createCondition(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition("on", 100)));
        List<Condition> conditions = Lists.newArrayList(rollOutToEveryone);
        Split parsedSplit = SplitHelper.createSplit(test, 123, true, Treatments.OFF, conditions, "user", 1, 1, null);

        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("adil@codigo.com"), splitsStorage);

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

        Condition parent_is_on = SplitHelper.createCondition(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition(Treatments.ON, 100)));
        List<Condition> parent_conditions = Lists.newArrayList(parent_is_on);
        Split parentSplit = SplitHelper.createSplit(parent, 123, false, Treatments.OFF, parent_conditions, null, 1, 1, null);

        Condition dependent_needs_parent = SplitHelper.createCondition(CombiningMatcher.of(new DependencyMatcher(parent, Lists.newArrayList(Treatments.ON))), Lists.newArrayList(partition(Treatments.ON, 100)));
        List<Condition> dependent_conditions = Lists.newArrayList(dependent_needs_parent);
        Split dependentSplit = SplitHelper.createSplit(dependent, 123, false, Treatments.OFF, dependent_conditions, null, 1, 1, null);

        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(parent)).thenReturn(parentSplit);
        when(splitsStorage.get(dependent)).thenReturn(dependentSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("key"), splitsStorage);

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

        Condition parent_is_on = SplitHelper.createCondition(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(partition(Treatments.ON, 100)));
        List<Condition> parent_conditions = Lists.newArrayList(parent_is_on);
        Split parentSplit = SplitHelper.createSplit(parent, 123, false, Treatments.OFF, parent_conditions, null, 1, 1, null);

        Condition dependent_needs_parent = SplitHelper.createCondition(CombiningMatcher.of(new DependencyMatcher(parent, Lists.newArrayList(Treatments.OFF))), Lists.newArrayList(partition(Treatments.ON, 100)));
        List<Condition> dependent_conditions = Lists.newArrayList(dependent_needs_parent);
        Split dependentSplit = SplitHelper.createSplit(dependent, 123, false, Treatments.OFF, dependent_conditions, null, 1, 1, null);

        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(parent)).thenReturn(parentSplit);
        when(splitsStorage.get(dependent)).thenReturn(dependentSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("key"), splitsStorage);

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

        Condition dependent_needs_parent = SplitHelper.createCondition(CombiningMatcher.of(new DependencyMatcher("not-exists", Lists.newArrayList(Treatments.OFF))), Lists.newArrayList(partition(Treatments.OFF, 100)));
        List<Condition> dependent_conditions = Lists.newArrayList(dependent_needs_parent);
        Split dependentSplit = SplitHelper.createSplit(dependent, 123, false, Treatments.ON, dependent_conditions, null, 1, 1, null);

        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(dependent)).thenReturn(dependentSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("key"), splitsStorage);

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

        Condition adil_is_always_on = SplitHelper.createCondition(CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@codigo.com"))), Lists.newArrayList(partition(Treatments.ON, 100)));
        Condition users_with_age_greater_than_10_are_on = SplitHelper.createCondition(CombiningMatcher.of("age", new GreaterThanOrEqualToMatcher(10, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<Condition> conditions = Lists.newArrayList(adil_is_always_on, users_with_age_greater_than_10_are_on);
        Split parsedSplit = SplitHelper.createSplit(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);

        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("adil@codigo.com"), splitsStorage);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test), is(equalTo("on")));
                assertThat(client.getTreatment(test, null), is(equalTo("on")));
                assertThat(client.getTreatment(test, ImmutableMap.of()), is(equalTo("on")));
            }
        });

        client = SplitClientImplFactory.get(Key.withMatchingKey("pato@codigo.com"), splitsStorage);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test, ImmutableMap.of("age", 10)), is(equalTo("on")));
                assertThat(client.getTreatment(test, ImmutableMap.of("age", 9)), is(equalTo("off")));

            }
        });
    }

    @Test
    public void attributes_work_2() {
        String test = "test1";

        Condition age_equal_to_0_should_be_on = SplitHelper.createCondition(CombiningMatcher.of("age", new EqualToMatcher(0, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<Condition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        Split parsedSplit = SplitHelper.createSplit(test, 123, false, Treatments.OFF, conditions, "user", 1, 1, null);

        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("adil@codigo.com"), splitsStorage);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test), is(equalTo("off")));
                assertThat(client.getTreatment(test, null), is(equalTo("off")));
                assertThat(client.getTreatment(test, ImmutableMap.of()), is(equalTo("off")));
            }
        });

        client = SplitClientImplFactory.get(Key.withMatchingKey("pato@codigo.com"), splitsStorage);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test, ImmutableMap.of("age", 10)), is(equalTo("off")));
                assertThat(client.getTreatment(test, ImmutableMap.of("age", 0)), is(equalTo("on")));
            }
        });
    }

    @Test
    public void attributes_greater_than_negative_number() {
        String test = "test1";

        Condition age_equal_to_0_should_be_on = SplitHelper.createCondition(CombiningMatcher.of("age", new EqualToMatcher(-20, DataType.NUMBER)), Lists.newArrayList(partition("on", 100)));

        List<Condition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        Split parsedSplit = SplitHelper.createSplit(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);

        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("adil@codigo.com"), splitsStorage);

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
        client = SplitClientImplFactory.get(Key.withMatchingKey("pato@codigo.com"), splitsStorage);

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

        Condition any_of_set = SplitHelper.createCondition(CombiningMatcher.of("products", new ContainsAnyOfSetMatcher(Lists.newArrayList("sms", "video"))), Lists.newArrayList(partition("on", 100)));

        List<Condition> conditions = Lists.newArrayList(any_of_set);
        Split parsedSplit = SplitHelper.createSplit(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);

        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(test)).thenReturn(parsedSplit);

        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("adil@codigo.com"), splitsStorage);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {

            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test), is(equalTo("off")));
                assertThat(client.getTreatment(test, null), is(equalTo("off")));
                assertThat(client.getTreatment(test, ImmutableMap.of()), is(equalTo("off")));
            }
        });

        client = SplitClientImplFactory.get(Key.withMatchingKey("pato@codigo.com"), splitsStorage);

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

        Condition age_equal_to_0_should_be_on = SplitHelper.createCondition(
                CombiningMatcher.of("age", new EqualToMatcher(-20, DataType.NUMBER)),
                Lists.newArrayList(partition("on", 100)));

        age_equal_to_0_should_be_on.conditionType = ConditionType.ROLLOUT;
        age_equal_to_0_should_be_on.label = "foolabel";

        List<Condition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        Split parsedSplit = SplitHelper.createSplit(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);
        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(test)).thenReturn(parsedSplit);
        ImpressionListener impressionListener = mock(ImpressionListener.class);
        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("pato@codigo.com"), splitsStorage, impressionListener);
        Map<String, Object> attributes = ImmutableMap.of("age", -20, "acv", "1000000");


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
        traffic_allocation(Key.withMatchingKey("pato@split.io"), 0, "off", "not in split");
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
            traffic_allocation(key, i, "off", "not in split");
        }

        for (; i <= 100; i++) {
            traffic_allocation(key, i, "on", "in segment all");
        }
    }

    @Test
    public void in_split_if_100_percent_allocation() {
        traffic_allocation(Key.withMatchingKey("pato@split.io"), 100, "on", "in segment all");
    }

    @Test
    public void in_split_if_1_percent_allocation() {
        traffic_allocation(Key.withMatchingKey("aaaaaaklmnbv"), 1, -1667452163, "on", "in segment all", 2);
    }

    @Test
    public void whitelist_overrides_traffic_allocation() {
        traffic_allocation(Key.withMatchingKey("adil@split.io"), 0, "on", "whitelisted user");
    }

    private void traffic_allocation(Key key, int trafficAllocation, String expected_treatment_on_or_off, String label) {
        traffic_allocation(key, trafficAllocation, 123, expected_treatment_on_or_off, label, 1);
    }

    private void traffic_allocation(Key key, int trafficAllocation, int trafficAllocationSeed, String expected_treatment_on_or_off, String label, int algo) {

        String test = "test1";

        Condition whitelistCondition = SplitHelper.createCondition(
                CombiningMatcher.of(new WhitelistMatcher(Lists.newArrayList("adil@split.io"))),
                Lists.newArrayList(partition("on", 100), partition("off", 0)));
        whitelistCondition.conditionType = ConditionType.WHITELIST;
        whitelistCondition.label = "whitelisted user";

        Condition rollOutToEveryone = SplitHelper.createCondition(
                CombiningMatcher.of(new AllKeysMatcher()),
                Lists.newArrayList(partition("on", 100), partition("off", 0)));
        rollOutToEveryone.conditionType = ConditionType.ROLLOUT;
        rollOutToEveryone.label = "in segment all";

        List<Condition> conditions = Lists.newArrayList(whitelistCondition, rollOutToEveryone);
        Split split = new Split();
        split.name = test;
        split.seed = 123;
        split.killed = false;
        split.defaultTreatment = Treatments.OFF;
        split.conditions = conditions;
        split.trafficTypeName = null;
        split.changeNumber = 1;
        split.trafficAllocation = trafficAllocation;
        split.trafficAllocationSeed = trafficAllocationSeed;
        split.algo = algo;

        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(test)).thenReturn(split);

        ImpressionListener impressionListener = mock(ImpressionListener.class);

        SplitClientImpl client = SplitClientImplFactory.get(key, splitsStorage, impressionListener);

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
        Condition aijaz_should_match = SplitHelper.createCondition(CombiningMatcher.of(new WhitelistMatcher(whitelist)), Lists.newArrayList(partition("on", 100)));

        List<Condition> conditions = Lists.newArrayList(aijaz_should_match);
        Split parsedSplit = SplitHelper.createSplit(test, 123, false, Treatments.OFF, conditions, "user", 1, 1, null);

        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(test)).thenReturn(parsedSplit);


        Key bad_key = new Key("adil", "aijaz");
        SplitClientImpl client = SplitClientImplFactory.get(bad_key, splitsStorage);

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test, Collections.emptyMap()), is(equalTo("off")));
            }
        });

        Key good_key = new Key("aijaz", "adil");
        client = SplitClientImplFactory.get(good_key, splitsStorage);


        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                assertThat(client.getTreatment(test, Collections.emptyMap()), is(equalTo("on")));
            }
        });

    }

    @Test
    public void impression_metadata_is_propagated() {
        String test = "test1";

        Condition age_equal_to_0_should_be_on = SplitHelper.createCondition(
                CombiningMatcher.of("age", new EqualToMatcher(-20, DataType.NUMBER)),
                Lists.newArrayList(partition("on", 100)));
        age_equal_to_0_should_be_on.conditionType = ConditionType.ROLLOUT;
        age_equal_to_0_should_be_on.label = "foolabel";

        List<Condition> conditions = Lists.newArrayList(age_equal_to_0_should_be_on);
        Split parsedSplit = SplitHelper.createSplit(test, 123, false, Treatments.OFF, conditions, null, 1, 1, null);

        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitsStorage.get(test)).thenReturn(parsedSplit);

        ImpressionListener impressionListener = mock(ImpressionListener.class);


        SplitClientImpl client = SplitClientImplFactory.get(Key.withMatchingKey("pato@codigo.com"), splitsStorage, impressionListener);

        Map<String, Object> attributes = ImmutableMap.of("age", -20, "acv", "1000000");

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
