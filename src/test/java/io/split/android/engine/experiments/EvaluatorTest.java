package io.split.android.engine.experiments;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.EvaluationResult;
import io.split.android.client.Evaluator;
import io.split.android.client.EvaluatorImpl;
import io.split.android.client.TreatmentLabels;
import io.split.android.client.dtos.Split;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.storage.TelemetryEvaluationProducer;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.grammar.Treatments;
import io.split.android.helpers.FileHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EvaluatorTest {

    private Evaluator evaluator;
    private final TelemetryStorageProducer telemetryStorageProducer = mock(TelemetryStorageProducer.class);

    @Before
    public void loadSplitsFromFile(){
        if(evaluator == null) {
            FileHelper fileHelper = new FileHelper();
            MySegmentsStorage mySegmentsStorage = mock(MySegmentsStorage.class);
            SplitsStorage splitsStorage = mock(SplitsStorage.class);

            Set<String> mySegments = new HashSet<>(Arrays.asList("s1", "s2", "test_copy"));
            List<Split> splits = fileHelper.loadAndParseSplitChangeFile("split_changes_1.json");
            SplitParser splitParser = new SplitParser(mySegmentsStorage);

            Map<String, Split> splitsMap = splitsMap(splits);
            when(splitsStorage.getAll()).thenReturn(splitsMap);
            when(splitsStorage.get("FACUNDO_TEST")).thenReturn(splitsMap.get("FACUNDO_TEST"));
            when(splitsStorage.get("a_new_split_2")).thenReturn(splitsMap.get("a_new_split_2"));
            when(splitsStorage.get("Test")).thenReturn(splitsMap.get("Test"));


            when(mySegmentsStorage.getAll()).thenReturn(mySegments);

            evaluator = new EvaluatorImpl(splitsStorage, splitParser);
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

    private Map<String, Split> splitsMap(List<Split> splits) {
        Map<String, Split> splitsMap = new HashMap<>();
        for(Split split : splits) {
            splitsMap.put(split.name, split);
        }
        return splitsMap;
    }
}
