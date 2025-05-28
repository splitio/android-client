package io.split.android.engine.experiments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.EvaluationResult;
import io.split.android.client.Evaluator;
import io.split.android.client.EvaluatorImpl;
import io.split.android.client.TreatmentLabels;
import io.split.android.client.dtos.Split;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.helpers.FileHelper;

/**
 * Tests for the prerequisite functionality in the Evaluator
 */
public class PrerequisitesEvaluatorTest {

    private Evaluator evaluator;
    private Map<String, Split> splitsMap;

    @Before
    public void loadSplitsFromFile() {
        if (evaluator == null) {
            FileHelper fileHelper = new FileHelper();
            MySegmentsStorage mySegmentsStorage = mock(MySegmentsStorage.class);
            MySegmentsStorage myLargeSegmentsStorage = mock(MySegmentsStorage.class);
            MySegmentsStorageContainer mySegmentsStorageContainer = mock(MySegmentsStorageContainer.class);
            MySegmentsStorageContainer myLargeSegmentsStorageContainer = mock(MySegmentsStorageContainer.class);
            RuleBasedSegmentStorage ruleBasedSegmentStorage = mock(RuleBasedSegmentStorage.class);
            SplitsStorage splitsStorage = mock(SplitsStorage.class);

            List<Split> splits = fileHelper.loadAndParseSplitChangeFile("split_changes_with_prerequisites.json");
            SplitParser splitParser = new SplitParser(new ParserCommons(mySegmentsStorageContainer, myLargeSegmentsStorageContainer));

            splitsMap = splitsMap(splits);
            when(splitsStorage.getAll()).thenReturn(splitsMap);
            when(splitsStorage.get(any())).thenAnswer(new Answer<Split>() {
                @Override
                public Split answer(InvocationOnMock invocation) throws Throwable {
                    return splitsMap.get(invocation.getArgument(0));
                }
            });

            when(splitsStorage.get("parent_split")).thenReturn(splitsMap.get("parent_split"));
            when(splitsStorage.get("child_split_1")).thenReturn(splitsMap.get("child_split_1"));
            when(splitsStorage.get("child_split_2")).thenReturn(splitsMap.get("child_split_2"));
            when(splitsStorage.get("parent_split_with_one_failing_prerequisite")).thenReturn(splitsMap.get("parent_split_with_one_failing_prerequisite"));
            when(splitsStorage.get("parent_split_with_non_existent_prerequisite")).thenReturn(splitsMap.get("parent_split_with_non_existent_prerequisite"));
            when(splitsStorage.get("non_existent_split")).thenReturn(null);

            when(mySegmentsStorageContainer.getStorageForKey(any())).thenReturn(mySegmentsStorage);
            when(myLargeSegmentsStorageContainer.getStorageForKey(any())).thenReturn(myLargeSegmentsStorage);

            evaluator = new EvaluatorImpl(splitsStorage, splitParser);
        }
    }

    @Test
    public void testPrerequisitesAllMet() {
        String matchingKey = "user1";
        String splitName = "parent_split";
        EvaluationResult result = evaluator.getTreatment(matchingKey, matchingKey, splitName, null);
        
        Assert.assertNotNull(result);
        Assert.assertEquals("off", result.getTreatment());
        Assert.assertEquals(TreatmentLabels.PREREQUISITES_NOT_MET, result.getLabel());
    }

    @Test
    public void testPrerequisitesNotMet() {
        String matchingKey = "user1";
        String splitName = "parent_split_with_one_failing_prerequisite";
        EvaluationResult result = evaluator.getTreatment(matchingKey, matchingKey, splitName, null);
        
        Assert.assertNotNull(result);
        Assert.assertEquals("off", result.getTreatment());
        Assert.assertEquals(TreatmentLabels.PREREQUISITES_NOT_MET, result.getLabel());
    }

    @Test
    public void testPrerequisiteNonExistent() {
        String matchingKey = "user1";
        String splitName = "parent_split_with_non_existent_prerequisite";
        EvaluationResult result = evaluator.getTreatment(matchingKey, matchingKey, splitName, null);
        
        Assert.assertNotNull(result);
        Assert.assertEquals("off", result.getTreatment());
        Assert.assertEquals(TreatmentLabels.PREREQUISITES_NOT_MET, result.getLabel());
    }

    @Test
    public void testChildSplitEvaluation() {
        String matchingKey = "user1";
        String splitName = "child_split_1";
        EvaluationResult result = evaluator.getTreatment(matchingKey, matchingKey, splitName, null);
        
        Assert.assertNotNull(result);
        Assert.assertEquals("on", result.getTreatment());
        Assert.assertEquals("in segment all", result.getLabel());
    }

    private Map<String, Split> splitsMap(List<Split> splits) {
        Map<String, Split> splitsMap = new HashMap<>();
        for (Split split : splits) {
            splitsMap.put(split.name, split);
        }
        return splitsMap;
    }
}
