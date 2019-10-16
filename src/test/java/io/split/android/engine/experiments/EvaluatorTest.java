package io.split.android.engine.experiments;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import io.split.android.client.EvaluationResult;
import io.split.android.client.Evaluator;
import io.split.android.client.EvaluatorImpl;
import io.split.android.client.TreatmentLabels;
import io.split.android.client.dtos.Split;
import io.split.android.engine.segments.RefreshableMySegmentsFetcherProvider;
import io.split.android.fake.RefreshableMySegmentsFetcherProviderStub;
import io.split.android.fake.SplitFetcherStub;
import io.split.android.grammar.Treatments;
import io.split.android.helpers.FileHelper;

public class EvaluatorTest {

    private SplitFetcher splitFetcher;
    private Evaluator evaluator;

    @Before
    public void loadSplitsFromFile(){
        if(splitFetcher == null) {
            FileHelper fileHelper = new FileHelper();
            List<String> mySegments = Arrays.asList("s1", "s2", "test_copy");
            RefreshableMySegmentsFetcherProvider mySegmentsProvider = new RefreshableMySegmentsFetcherProviderStub(mySegments);
            List<Split> splits = fileHelper.loadAndParseSplitChangeFile("split_changes_1.json");
            splitFetcher = new SplitFetcherStub(splits, mySegmentsProvider);
            evaluator = new EvaluatorImpl(splitFetcher);
        }
    }

    @Test
    public void testWhitelisted() {
        String matchingKey = "nico_test";
        String splitName = "FACUNDO_TEST";
        EvaluationResult result = evaluator.getTreatment(matchingKey, matchingKey, splitName, null);
        Assert.assertNotNull(result);
        Assert.assertEquals("on", result.getTreatment());
        Assert.assertEquals("whitelisted", result.getLabel());
    }

    @Test
    public void testWhitelistedOff() {
        String matchingKey = "bla";
        String splitName = "FACUNDO_TEST";
        EvaluationResult result = evaluator.getTreatment(matchingKey, matchingKey, splitName, null);
        Assert.assertNotNull(result);
        Assert.assertEquals("off", result.getTreatment());
        Assert.assertNull(result.getConfigurations());
        Assert.assertEquals("whitelisted", result.getLabel());
    }

    @Test
    public void testDefaultTreatmentFacundo() {
        String matchingKey = "anyKey";
        String splitName = "FACUNDO_TEST";
        EvaluationResult result = evaluator.getTreatment(matchingKey, matchingKey, splitName, null);
        Assert.assertNotNull(result);
        Assert.assertEquals("off", result.getTreatment());
        Assert.assertNull(result.getConfigurations());
        Assert.assertEquals("in segment all", result.getLabel());
    }

    @Test
    public void testInSegmentTestKey() {
        String matchingKey = "anyKey";
        String splitName = "a_new_split_2";
        EvaluationResult result = evaluator.getTreatment(matchingKey, matchingKey, splitName, null);
        Assert.assertNotNull(result);
        Assert.assertEquals("off", result.getTreatment());
        Assert.assertNull(result.getConfigurations());
        Assert.assertEquals("whitelisted segment", result.getLabel());
    }

    @Test
    public void testKilledSplit() {
        String matchingKey = "anyKey";
        String splitName = "Test";
        EvaluationResult result = evaluator.getTreatment(matchingKey, matchingKey, splitName, null);
        Assert.assertNotNull(result);
        Assert.assertEquals("off", result.getTreatment());
        Assert.assertNotNull(result.getConfigurations());
        Assert.assertEquals(TreatmentLabels.KILLED, result.getLabel());
    }

    @Test
    public void testNotInSplit() {
        String matchingKey = "anyKey";
        String splitName = "split_not_available_to_test_right_now";
        EvaluationResult result = evaluator.getTreatment(matchingKey, matchingKey, splitName, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(Treatments.CONTROL, result.getTreatment());
        Assert.assertNull(result.getConfigurations());
        Assert.assertEquals(TreatmentLabels.DEFINITION_NOT_FOUND, result.getLabel());
    }

    @Test
    public void testBrokenSplit() {
        String matchingKey = "anyKey";
        String splitName = "broken_split";
        EvaluationResult result = evaluator.getTreatment(matchingKey, matchingKey, splitName, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(Treatments.CONTROL, result.getTreatment());
        Assert.assertNull(result.getConfigurations());
        Assert.assertEquals(TreatmentLabels.DEFINITION_NOT_FOUND, result.getLabel());
    }




}
