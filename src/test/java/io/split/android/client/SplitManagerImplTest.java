package io.split.android.client;

import com.google.common.collect.Lists;
import io.split.android.client.api.SplitView;
import io.split.android.engine.ConditionsTestUtil;
import io.split.android.engine.experiments.ParsedCondition;
import io.split.android.engine.experiments.ParsedSplit;
import io.split.android.engine.experiments.SplitFetcher;
import io.split.android.engine.matchers.AllKeysMatcher;
import io.split.android.engine.matchers.CombiningMatcher;
import io.split.android.helpers.SplitHelper;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class SplitManagerImplTest {

    @Test
    public void splitCallWithNonExistentSplit() {
        String nonExistent = "nonExistent";
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        Mockito.when(splitFetcher.fetch(nonExistent)).thenReturn(null);
        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher);
        assertThat(splitManager.split("nonExistent"), is(nullValue()));
    }

    @Test
    public void splitCallWithExistentSplit() {
        String existent = "existent";
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);

        Map<String, String> configs = new HashMap<>();
        configs.put("off", "{\"f\":\"v\"}");
        configs.put("on", "{\"f1\":\"v1\"}");
        ParsedSplit response = ParsedSplit.createParsedSplitForTests("FeatureName", 123, true, "off", Lists.newArrayList(getTestCondition("off")), "traffic", 456L, 1, configs);
        Mockito.when(splitFetcher.fetch(existent)).thenReturn(response);

        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher);
        SplitView theOne = splitManager.split(existent);
        assertThat(theOne.name, is(equalTo(response.feature())));
        assertThat(theOne.changeNumber, is(equalTo(response.changeNumber())));
        assertThat(theOne.killed, is(equalTo(response.killed())));
        assertThat(theOne.trafficType, is(equalTo(response.trafficTypeName())));
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
        Mockito.when(splitFetcher.fetchAll()).thenReturn(Lists.<ParsedSplit>newArrayList());
        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher);
        assertThat(splitManager.splits(), is(empty()));
    }

    @Test
    public void splitsCallWithSplit() {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        List<ParsedSplit> parsedSplits = Lists.newArrayList();
        ParsedSplit response = ParsedSplit.createParsedSplitForTests("FeatureName", 123, true, "off", Lists.newArrayList(getTestCondition("off")), "traffic", 456L, 1, null);
        parsedSplits.add(response);

        Mockito.when(splitFetcher.fetchAll()).thenReturn(parsedSplits);
        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher);
        List<SplitView> splits = splitManager.splits();
        assertThat(splits.size(), is(equalTo(1)));
        assertThat(splits.get(0).name, is(equalTo(response.feature())));
        assertThat(splits.get(0).changeNumber, is(equalTo(response.changeNumber())));
        assertThat(splits.get(0).killed, is(equalTo(response.killed())));
        assertThat(splits.get(0).trafficType, is(equalTo(response.trafficTypeName())));
        assertThat(splits.get(0).treatments.size(), is(equalTo(1)));
        assertThat(splits.get(0).treatments.get(0), is(equalTo("off")));
        assertThat(splits.get(0).treatments.get(0), is(equalTo("off")));
    }

    @Test
    public void splitNamesCallWithNoSplit() {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        Mockito.when(splitFetcher.fetchAll()).thenReturn(Lists.<ParsedSplit>newArrayList());
        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher);
        assertThat(splitManager.splitNames(), is(empty()));
    }

    @Test
    public void splitNamesCallWithSplit() {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        List<ParsedSplit> parsedSplits = Lists.newArrayList();
        ParsedSplit response = ParsedSplit.createParsedSplitForTests("FeatureName", 123, true, "off", Lists.newArrayList(getTestCondition("off")), "traffic", 456L, 1, null);
        parsedSplits.add(response);

        Mockito.when(splitFetcher.fetchAll()).thenReturn(parsedSplits);
        SplitManagerImpl splitManager = new SplitManagerImpl(splitFetcher);
        List<String> splitNames = splitManager.splitNames();
        assertThat(splitNames.size(), is(equalTo(1)));
        assertThat(splitNames.get(0), is(equalTo(response.feature())));
    }

    private ParsedCondition getTestCondition(String treatment) {
        return ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(ConditionsTestUtil.partition(treatment, 10)));
    }

}
