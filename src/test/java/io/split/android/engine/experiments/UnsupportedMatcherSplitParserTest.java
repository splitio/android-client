package io.split.android.engine.experiments;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import io.split.android.client.dtos.Split;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.client.utils.Json;

public class UnsupportedMatcherSplitParserTest {

    @Mock
    private MySegmentsStorage mMySegmentsStorage;
    @Mock
    private MySegmentsStorageContainer mMySegmentsStorageContainer;
    @Mock
    private MySegmentsStorageContainer mMyLargeSegmentsStorageContainer;
    @Mock
    private DefaultConditionsProvider mDefaultConditionsProvider;
    @Mock
    private RuleBasedSegmentStorage mRuleBasedSegmentStorage;
    private final Split mTestFlag = Json.fromJson("{\"changeNumber\":1709843458770,\"trafficTypeName\":\"user\",\"name\":\"feature_flag_for_test\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-1364119282,\"seed\":-605938843,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"algo\":2,\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":null},\"matcherType\":\"WRONG_MATCHER_TYPE\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"dependencyMatcherData\":null,\"booleanMatcherData\":null,\"stringMatcherData\":\"123123\"}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100}],\"label\":\"wrong matcher type\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":\"sem\"},\"matcherType\":\"MATCHES_STRING\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"dependencyMatcherData\":null,\"booleanMatcherData\":null,\"stringMatcherData\":\"1.2.3\"}]},\"partitions\":[{\"treatment\":\"on\",\"size\":100},{\"treatment\":\"off\",\"size\":0}],\"label\":\"sem matches 1.2.3\"}],\"configurations\":{},\"sets\":[]}", Split.class);
    private AutoCloseable mAutoCloseable;
    private SplitParser mSplitParser;

    @Before
    public void setUp() {
        mAutoCloseable = MockitoAnnotations.openMocks(this);
        when(mMySegmentsStorageContainer.getStorageForKey("")).thenReturn(mMySegmentsStorage);
        when(mMySegmentsStorage.getAll()).thenReturn(Collections.emptySet());
        when(mDefaultConditionsProvider.getDefaultConditions()).thenReturn(Collections.emptyList());
        mSplitParser = new SplitParser(
                new ParserCommons(mMySegmentsStorageContainer, mMyLargeSegmentsStorageContainer, mRuleBasedSegmentStorage, mDefaultConditionsProvider));
    }

    @After
    public void tearDown() throws Exception {
        mAutoCloseable.close();
    }

    @Test
    public void parsingFeatureFlagWithUnsupportedMatcherDoesNotReturnNull() {
        ParsedSplit parsedSplit = mSplitParser.parse(mTestFlag);

        assertNotNull(parsedSplit);
    }

    @Test
    public void parsingFeatureFlagWithUnsupportedMatcherUsesDefaultConditionsProvider() {
        mSplitParser.parse(mTestFlag);

        verify(mDefaultConditionsProvider).getDefaultConditions();
    }
}
