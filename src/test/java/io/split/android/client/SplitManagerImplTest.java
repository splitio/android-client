package io.split.android.client;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.split.android.client.api.SplitView;
import io.split.android.client.dtos.Condition;
import io.split.android.client.dtos.Prerequisite;
import io.split.android.client.dtos.Split;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.engine.ConditionsTestUtil;
import io.split.android.engine.experiments.ParserCommons;
import io.split.android.engine.experiments.SplitFetcher;
import io.split.android.engine.experiments.SplitParser;
import io.split.android.engine.matchers.AllKeysMatcher;
import io.split.android.engine.matchers.CombiningMatcher;
import io.split.android.helpers.SplitHelper;

public class SplitManagerImplTest {

    @Mock
    SplitsStorage mSplitsStorage;
    @Mock
    MySegmentsStorage mMySegmentsStorage;
    @Mock
    MySegmentsStorageContainer mMySegmentsStorageContainer;
    @Mock
    MySegmentsStorageContainer mMyLargeSegmentsStorageContainer;
    @Mock
    RuleBasedSegmentStorage mRuleBasedSegmentStorage;
    @Mock
    SplitManager mSplitManager;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        SplitValidator validator = new SplitValidatorImpl();
        when(mMySegmentsStorageContainer.getStorageForKey("")).thenReturn(mMySegmentsStorage);
        SplitParser parser = new SplitParser(new ParserCommons(mMySegmentsStorageContainer, mMyLargeSegmentsStorageContainer));
        mSplitManager = new SplitManagerImpl(mSplitsStorage, validator, parser);
    }

    @Test
    public void splitCallWithNonExistentSplit() {
        String nonExistent = "nonExistent";
        when(mSplitsStorage.get(nonExistent)).thenReturn(null);
        assertThat(mSplitManager.split("nonExistent"), is(nullValue()));
    }

    @Test
    public void splitCallWithExistentSplit() {
        String existent = "existent";

        Map<String, String> configs = new HashMap<>();
        configs.put("off", "{\"f\":\"v\"}");
        configs.put("on", "{\"f1\":\"v1\"}");
        Split response = SplitHelper.createSplit("existent", 123,
                true, "off", Arrays.asList(getTestCondition()),
                "traffic", 456L, 1, configs);
        when(mSplitsStorage.get(existent)).thenReturn(response);

        SplitManager splitManager = mSplitManager;
        SplitView theOne = splitManager.split(existent);
        assertThat(theOne.name, is(equalTo(response.name)));
        assertThat(theOne.changeNumber, is(equalTo(response.changeNumber)));
        assertThat(theOne.killed, is(equalTo(response.killed)));
        assertThat(theOne.trafficType, is(equalTo(response.trafficTypeName)));
        assertThat(theOne.treatments.size(), is(equalTo(1)));
        assertThat(theOne.treatments.get(0), is(equalTo("off")));
        assertThat(theOne.configs, is(notNullValue()));
        assertThat(theOne.configs.get("off"), is(notNullValue()));
        assertThat(theOne.configs.get("off"), is(equalTo("{\"f\":\"v\"}")));
        assertThat(theOne.configs.get("on"), is(notNullValue()));
        assertThat(theOne.configs.get("on"), is(equalTo("{\"f1\":\"v1\"}")));
        assertThat(theOne.configs.get("onOff"), is(nullValue()));
    }

    @Test
    public void splitsCallWithNoSplit() {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        when(splitFetcher.fetchAll()).thenReturn(Arrays.asList());
        SplitManager splitManager = mSplitManager;
        assertThat(splitManager.splits(), is(empty()));
    }

    @Test
    public void splitsCallWithSplit() {
        Map<String, Split> splitsMap = new HashMap<>();
        Split split = SplitHelper.createSplit("FeatureName", 123, true, "off", Arrays.asList(getTestCondition()), "traffic", 456L, 1, null);
        splitsMap.put(split.name, split);

        when(mSplitsStorage.getAll()).thenReturn(splitsMap);
        SplitManager splitManager = mSplitManager;
        List<SplitView> splits = splitManager.splits();
        assertThat(splits.size(), is(equalTo(1)));
        assertThat(splits.get(0).name, is(equalTo(split.name)));
        assertThat(splits.get(0).changeNumber, is(equalTo(split.changeNumber)));
        assertThat(splits.get(0).killed, is(equalTo(split.killed)));
        assertThat(splits.get(0).trafficType, is(equalTo(split.trafficTypeName)));
        assertThat(splits.get(0).treatments.size(), is(equalTo(1)));
        assertThat(splits.get(0).treatments.get(0), is(equalTo("off")));
        assertThat(splits.get(0).treatments.get(0), is(equalTo("off")));
    }

    @Test
    public void splitNamesCallWithNoSplit() {
        when(mSplitsStorage.getAll()).thenReturn(new HashMap<>());
        SplitManager splitManager = mSplitManager;
        assertThat(splitManager.splitNames(), is(empty()));
    }

    @Test
    public void splitNamesCallWithSplit() {
        Map<String, Split> splitsMap = new HashMap<>();
        Split split = SplitHelper.createSplit("FeatureName", 123, true,
                "off", Arrays.asList(getTestCondition()),
                "traffic", 456L, 1, null);
        splitsMap.put(split.name, split);

        when(mSplitsStorage.getAll()).thenReturn(splitsMap);
        SplitManager splitManager = mSplitManager;
        List<String> splitNames = splitManager.splitNames();
        assertThat(splitNames.size(), is(equalTo(1)));
        assertThat(splitNames.get(0), is(equalTo(split.name)));
    }

    @Test
    public void flagSets() {
        Map<String, Split> splitsMap = new HashMap<>();
        Split split = SplitHelper.createSplit("FeatureName", 123, true,
                "off", Arrays.asList(getTestCondition()),
                "traffic", 456L, 1, null);
        splitsMap.put(split.name, split);
        when(mSplitsStorage.getAll()).thenReturn(splitsMap);

        SplitManager splitManager = mSplitManager;

        List<SplitView> splitNames = splitManager.splits();
        assertEquals(1, splitNames.size());
        assertEquals(split.name, splitNames.get(0).name);
        assertEquals(new ArrayList<>(split.sets), splitNames.get(0).sets);
    }

    @Test
    public void defaultTreatmentIsPresent() {
        Split split = SplitHelper.createSplit("FeatureName", 123, true,
                "some_treatment", Arrays.asList(getTestCondition()),
                "traffic", 456L, 1, null);
        when(mSplitsStorage.get("FeatureName")).thenReturn(split);

        SplitView featureFlag = mSplitManager.split("FeatureName");

        assertEquals("some_treatment", featureFlag.defaultTreatment);
    }

    @Test
    public void defaultTreatmentIsPresentWhenFetchingMultipleSplits() {
        Map<String, Split> splitsMap = new HashMap<>();
        Split split = SplitHelper.createSplit("FeatureName", 123, true,
                "some_treatment", Arrays.asList(getTestCondition()),
                "traffic", 456L, 1, null);
        splitsMap.put(split.name, split);
        when(mSplitsStorage.getAll()).thenReturn(splitsMap);

        List<SplitView> splitNames = mSplitManager.splits();

        assertEquals(1, splitNames.size());
        assertEquals("some_treatment", splitNames.get(0).defaultTreatment);
    }

    @Test
    public void impressionsDisabledIsPresent() {
        Split split = SplitHelper.createSplit("FeatureName", 123, true,
                "some_treatment", Arrays.asList(getTestCondition()),
                "traffic", 456L, 1, null);
        split.impressionsDisabled = false;
        when(mSplitsStorage.get("FeatureName")).thenReturn(split);

        SplitView featureFlag = mSplitManager.split("FeatureName");

        assertFalse(featureFlag.impressionsDisabled);
    }

    @Test
    public void prerequisitesIsPresent() {
        Split split = SplitHelper.createSplit("FeatureName", 123, true,
                "some_treatment", Arrays.asList(getTestCondition()),
                "traffic", 456L, 1, null);
        Prerequisite p1 = new Prerequisite("prereq_feature", Collections.singleton("some_treatment"));
        HashSet<String> treatments = new HashSet<>();
        treatments.add("some_treatment_1");
        treatments.add("some_other_treatment");
        Prerequisite p2 = new Prerequisite("prereq_feature_2", treatments);
        split.prerequisites = Arrays.asList(p1, p2);

        when(mSplitsStorage.get("FeatureName")).thenReturn(split);

        SplitView featureFlag = mSplitManager.split("FeatureName");

        List<Prerequisite> prerequisites = featureFlag.prerequisites;
        Prerequisite prereq1 = prerequisites.get(0);
        Prerequisite prereq2 = prerequisites.get(1);
        assertEquals(2, prerequisites.size());
        assertEquals("prereq_feature", prereq1.getFlagName());
        assertEquals(1, prereq1.getTreatments().size());
        assertEquals("some_treatment", prereq1.getTreatments().iterator().next());
        assertEquals("prereq_feature_2", prereq2.getFlagName());
        assertEquals(2, prereq2.getTreatments().size());
        assertTrue(prereq2.getTreatments().contains("some_treatment_1"));
        assertTrue(prereq2.getTreatments().contains("some_other_treatment"));
    }

    @Test
    public void nullPrerequisitesDefaultToEmptyList() {
        Split split = SplitHelper.createSplit("FeatureName", 123, true,
                "some_treatment", Arrays.asList(getTestCondition()),
                "traffic", 456L, 1, null);
        split.prerequisites = null;
        when(mSplitsStorage.get("FeatureName")).thenReturn(split);

        SplitView featureFlag = mSplitManager.split("FeatureName");

        List<Prerequisite> prerequisites = featureFlag.prerequisites;
        assertEquals(0, prerequisites.size());
    }

    private Condition getTestCondition() {
        return SplitHelper.createCondition(CombiningMatcher.of(new AllKeysMatcher()), Arrays.asList(ConditionsTestUtil.partition("off", 10)));
    }
}
