package io.split.android.client;

import com.google.common.collect.Lists;
import io.split.android.client.api.SplitView;
import io.split.android.client.dtos.Condition;
import io.split.android.client.dtos.Split;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.engine.ConditionsTestUtil;
import io.split.android.engine.experiments.SplitFetcher;
import io.split.android.engine.experiments.SplitParser;
import io.split.android.engine.matchers.AllKeysMatcher;
import io.split.android.engine.matchers.CombiningMatcher;
import io.split.android.helpers.SplitHelper;


import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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

    SplitsStorage mSplitsStorage;
    MySegmentsStorage mMySegmentsStorage;
    SplitManager mSplitManager;

    @Before
    public void setup() {
        mSplitsStorage = Mockito.mock(SplitsStorage.class);
        mMySegmentsStorage = Mockito.mock(MySegmentsStorage.class);
        SplitValidator validator = new SplitValidatorImpl();
        SplitParser parser = new SplitParser(mMySegmentsStorage);
        mSplitManager = new SplitManagerImpl(mSplitsStorage, validator, parser);
    }

    @Test
    public void splitCallWithNonExistentSplit() {
        String nonExistent = "nonExistent";
        Mockito.when(mSplitsStorage.get(nonExistent)).thenReturn(null);
        assertThat(mSplitManager.split("nonExistent"), is(nullValue()));
    }

    @Test
    public void splitCallWithExistentSplit() {
        String existent = "existent";
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);

        Map<String, String> configs = new HashMap<>();
        configs.put("off", "{\"f\":\"v\"}");
        configs.put("on", "{\"f1\":\"v1\"}");
        Split response = SplitHelper.createSplit("existent", 123,
                true, "off", Lists.newArrayList(getTestCondition()),
                "traffic", 456L, 1, configs);
        Mockito.when(mSplitsStorage.get(existent)).thenReturn(response);

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
        Mockito.when(splitFetcher.fetchAll()).thenReturn(Lists.newArrayList());
        SplitManager splitManager = mSplitManager;
        assertThat(splitManager.splits(), is(empty()));
    }

    @Test
    public void splitsCallWithSplit() {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        Map<String, Split> splitsMap = new HashMap<>();
        Split split = SplitHelper.createSplit("FeatureName", 123, true, "off", Lists.newArrayList(getTestCondition()), "traffic", 456L, 1, null);
        splitsMap.put(split.name, split);

        Mockito.when(mSplitsStorage.getAll()).thenReturn(splitsMap);
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
        Mockito.when(mSplitsStorage.getAll()).thenReturn(new HashMap<>());
        SplitManager splitManager = mSplitManager;
        assertThat(splitManager.splitNames(), is(empty()));
    }

    @Test
    public void splitNamesCallWithSplit() {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        Map<String, Split> splitsMap = new HashMap<>();
        Split split = SplitHelper.createSplit("FeatureName", 123, true,
                "off", Lists.newArrayList(getTestCondition()),
                "traffic", 456L, 1, null);
        splitsMap.put(split.name, split);

        Mockito.when(mSplitsStorage.getAll()).thenReturn(splitsMap);
        SplitManager splitManager = mSplitManager;
        List<String> splitNames = splitManager.splitNames();
        assertThat(splitNames.size(), is(equalTo(1)));
        assertThat(splitNames.get(0), is(equalTo(split.name)));
    }

    private Condition getTestCondition() {
        return SplitHelper.createCondition(CombiningMatcher.of(new AllKeysMatcher()), Lists.newArrayList(ConditionsTestUtil.partition("off", 10)));
    }

}
