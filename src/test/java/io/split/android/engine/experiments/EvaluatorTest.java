package io.split.android.engine.experiments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
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
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorageProvider;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.grammar.Treatments;
import io.split.android.helpers.FileHelper;

public class EvaluatorTest {

    private Evaluator evaluator;

    @Before
    public void loadSplitsFromFile() {
        if (evaluator == null) {
            FileHelper fileHelper = new FileHelper();
            MySegmentsStorage mySegmentsStorage = mock(MySegmentsStorage.class);
            MySegmentsStorage myLargeSegmentsStorage = mock(MySegmentsStorage.class);
            MySegmentsStorageContainer mySegmentsStorageContainer = mock(MySegmentsStorageContainer.class);
            MySegmentsStorageContainer myLargeSegmentsStorageContainer = mock(MySegmentsStorageContainer.class);
            RuleBasedSegmentStorage ruleBasedSegmentStorage = mock(RuleBasedSegmentStorage.class);
            RuleBasedSegmentStorageProvider ruleBasedSegmentStorageProvider = mock(RuleBasedSegmentStorageProvider.class);
            when(ruleBasedSegmentStorageProvider.get()).thenReturn(ruleBasedSegmentStorage);
            SplitsStorage splitsStorage = mock(SplitsStorage.class);

            Set<String> mySegments = new HashSet<>(Arrays.asList("s1", "s2", "test_copy"));
            Set<String> myLargeSegments = new HashSet<>(Arrays.asList("segment1"));
            List<Split> splits = fileHelper.loadAndParseSplitChangeFile("split_changes_1.json");
            SplitParser splitParser = new SplitParser(new ParserCommons(mySegmentsStorageContainer, myLargeSegmentsStorageContainer, ruleBasedSegmentStorageProvider));

            Map<String, Split> splitsMap = splitsMap(splits);
            when(splitsStorage.getAll()).thenReturn(splitsMap);
            when(splitsStorage.get(any())).thenAnswer(new Answer<Split>() {
                @Override
                public Split answer(InvocationOnMock invocation) throws Throwable {
                    return splitsMap.get(invocation.getArgument(0));
                }
            });


            when(splitsStorage.get("FACUNDO_TEST")).thenReturn(splitsMap.get("FACUNDO_TEST"));
            when(splitsStorage.get("a_new_split_2")).thenReturn(splitsMap.get("a_new_split_2"));
            when(splitsStorage.get("Test")).thenReturn(splitsMap.get("Test"));
            when(splitsStorage.get("ls_split")).thenReturn(splitsMap.get("ls_split"));

            when(mySegmentsStorageContainer.getStorageForKey(any())).thenReturn(mySegmentsStorage);
            when(myLargeSegmentsStorageContainer.getStorageForKey("anyKey")).thenReturn(myLargeSegmentsStorage);
            when(mySegmentsStorage.getAll()).thenReturn(mySegments);
            when(myLargeSegmentsStorage.getAll()).thenReturn(myLargeSegments);

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
    public void testInLargeSegmentKey() {
        String matchingKey = "anyKey";
        String splitName = "ls_split";
        EvaluationResult result = evaluator.getTreatment(matchingKey, matchingKey, splitName, null);
        Assert.assertNotNull(result);
        Assert.assertEquals("on", result.getTreatment());
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

    @Test
    public void exceptionInParsingReturnsResultWithExceptionLabelAndNoChangeNumber() {
        SplitParser splitParser = mock(SplitParser.class);
        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        when(splitParser.parse(any(), any())).thenThrow(new RuntimeException("test"));
        Evaluator evaluator = new EvaluatorImpl(splitsStorage, splitParser);

        String matchingKey = "anyKey";
        String splitName = "a_new_split_2";

        EvaluationResult result = evaluator.getTreatment(matchingKey, matchingKey, splitName, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(Treatments.CONTROL, result.getTreatment());
        Assert.assertNull(result.getConfigurations());
        Assert.assertEquals("exception", result.getLabel());
        Assert.assertNull(result.getChangeNumber());
    }

    @Test
    public void changeNumberExceptionReturnsResultWithExceptionLabelAndChangeNumber() {
        SplitParser splitParser = mock(SplitParser.class);
        SplitsStorage splitsStorage = mock(SplitsStorage.class);
        ParsedSplit parsedSplit = mock(ParsedSplit.class);
        when(parsedSplit.killed()).thenThrow(new RuntimeException("test"));
        when(parsedSplit.changeNumber()).thenReturn(123L);
        when(splitParser.parse(any(), any())).thenReturn(parsedSplit);

        Evaluator evaluator = new EvaluatorImpl(splitsStorage, splitParser);

        String matchingKye = "anyKey";
        String splitName = "a_new_split_2";

        EvaluationResult result = evaluator.getTreatment(matchingKye, matchingKye, splitName, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(Treatments.CONTROL, result.getTreatment());
        Assert.assertNull(result.getConfigurations());
        Assert.assertEquals("exception", result.getLabel());
        Assert.assertEquals(Long.valueOf(123), result.getChangeNumber());
    }

    private Map<String, Split> splitsMap(List<Split> splits) {
        Map<String, Split> splitsMap = new HashMap<>();
        for (Split split : splits) {
            splitsMap.put(split.name, split);
        }
        return splitsMap;
    }
}
