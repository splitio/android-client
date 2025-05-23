package io.split.android.engine.matchers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.split.android.client.EvaluationResult;
import io.split.android.client.Evaluator;
import io.split.android.client.dtos.Condition;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Prerequisite;
import io.split.android.client.dtos.Split;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.helpers.SplitHelper;

public class PrerequisitesMatcherTest {

    @Mock
    private SplitsStorage mSplitsStorage;

    @Mock
    private Evaluator mEvaluator;

    private final Map<String, Split> mStoredSplits = new HashMap<>();

    @Before
    public void setUp() {
        mSplitsStorage = mock(SplitsStorage.class);
        mEvaluator = mock(Evaluator.class);
        
        List<Partition> alwaysOnPartitions = createPartitions("on", 100, "off", 0);
        List<Condition> alwaysOnConditions = new ArrayList<>();
        alwaysOnConditions.add(SplitHelper.createCondition(null, alwaysOnPartitions));
        
        Split alwaysOn = SplitHelper.createSplit(
                "always-on",
                -725161385,
                false,
                "off",
                alwaysOnConditions,
                "user",
                1494364996459L,
                1,
                null);
        
        List<Partition> alwaysOffPartitions = createPartitions("on", 0, "off", 100);
        List<Condition> alwaysOffConditions = new ArrayList<>();
        alwaysOffConditions.add(SplitHelper.createCondition(null, alwaysOffPartitions));
        
        Split alwaysOff = SplitHelper.createSplit(
                "always-off",
                403891040,
                false,
                "on",
                alwaysOffConditions,
                "user",
                1494365020316L,
                1,
                null);
        
        mStoredSplits.put("always-on", alwaysOn);
        mStoredSplits.put("always-off", alwaysOff);
        
        when(mSplitsStorage.get(eq("always-on"))).thenReturn(mStoredSplits.get("always-on"));
        when(mSplitsStorage.get(eq("always-off"))).thenReturn(mStoredSplits.get("always-off"));
        when(mSplitsStorage.get(eq("not-existent-feature-flag"))).thenReturn(null);
        
        when(mEvaluator.getTreatment(any(), any(), eq("always-on"), any()))
            .thenReturn(new EvaluationResult("on", "in segment all"));
        
        when(mEvaluator.getTreatment(any(), any(), eq("always-off"), any()))
            .thenReturn(new EvaluationResult("off", "in segment all"));
        
        when(mEvaluator.getTreatment(any(), any(), eq("not-existent-feature-flag"), any()))
            .thenReturn(null);
    }
    
    private List<Partition> createPartitions(String treatment1, int size1, String treatment2, int size2) {
        List<Partition> partitions = new ArrayList<>();
        
        Partition partition1 = new Partition();
        partition1.treatment = treatment1;
        partition1.size = size1;
        partitions.add(partition1);
        
        Partition partition2 = new Partition();
        partition2.treatment = treatment2;
        partition2.size = size2;
        partitions.add(partition2);
        
        return partitions;
    }

    @Test
    public void shouldReturnTrueWhenSinglePrerequisiteIsMetForAlwaysOn() {
        List<Prerequisite> prerequisites = new ArrayList<>();
        prerequisites.add(new Prerequisite("always-on", new HashSet<>(Arrays.asList("not-existing", "on", "other"))));
        PrerequisitesMatcher matcher = new PrerequisitesMatcher(prerequisites);
        
        assertTrue(matcher.match("a-key", null, null, mEvaluator));
    }
    
    @Test
    public void shouldReturnFalseWhenSinglePrerequisiteIsNotMetForAlwaysOn() {
        List<Prerequisite> prerequisites = new ArrayList<>();
        prerequisites.add(new Prerequisite("always-on", new HashSet<>(Arrays.asList("off", "v1"))));
        PrerequisitesMatcher matcher = new PrerequisitesMatcher(prerequisites);
        
        assertFalse(matcher.match("a-key", null, null, mEvaluator));
    }
    
    @Test
    public void shouldReturnTrueWhenSinglePrerequisiteIsMetForAlwaysOff() {
        List<Prerequisite> prerequisites = new ArrayList<>();
        prerequisites.add(new Prerequisite("always-off", new HashSet<>(Arrays.asList("not-existing", "off"))));
        PrerequisitesMatcher matcher = new PrerequisitesMatcher(prerequisites);
        
        assertTrue(matcher.match("a-key", null, null, mEvaluator));
    }
    
    @Test
    public void shouldReturnFalseWhenSinglePrerequisiteIsNotMetForAlwaysOff() {
        List<Prerequisite> prerequisites = new ArrayList<>();
        prerequisites.add(new Prerequisite("always-off", new HashSet<>(Arrays.asList("v1", "on"))));
        PrerequisitesMatcher matcher = new PrerequisitesMatcher(prerequisites);
        
        assertFalse(matcher.match("a-key", null, null, mEvaluator));
    }
    
    @Test
    public void shouldReturnTrueWhenAllMultiplePrerequisitesAreMet() {
        List<Prerequisite> prerequisites = new ArrayList<>();
        prerequisites.add(new Prerequisite("always-on", new HashSet<>(Collections.singletonList("on"))));
        prerequisites.add(new Prerequisite("always-off", new HashSet<>(Collections.singletonList("off"))));
        PrerequisitesMatcher matcher = new PrerequisitesMatcher(prerequisites);
        
        assertTrue(matcher.match("a-key", null, null, mEvaluator));
    }
    
    @Test
    public void shouldReturnFalseWhenOneOfMultiplePrerequisitesIsNotMet() {
        List<Prerequisite> prerequisites = new ArrayList<>();
        prerequisites.add(new Prerequisite("always-on", new HashSet<>(Collections.singletonList("on"))));
        prerequisites.add(new Prerequisite("always-off", new HashSet<>(Collections.singletonList("on"))));
        PrerequisitesMatcher matcher = new PrerequisitesMatcher(prerequisites);
        
        assertFalse(matcher.match("a-key", null, null, mEvaluator));
    }

    @Test
    public void shouldReturnTrueWithNullPrerequisites() {
        PrerequisitesMatcher matcher = new PrerequisitesMatcher(null);
        
        assertTrue(matcher.match("a-key", null, null, mEvaluator));
    }
    
    @Test
    public void shouldReturnTrueWithEmptyPrerequisites() {
        PrerequisitesMatcher matcher = new PrerequisitesMatcher(new ArrayList<>());
        
        assertTrue(matcher.match("a-key", null, null, mEvaluator));
    }
    
    @Test
    public void shouldReturnFalseWhenFeatureFlagDoesNotExist() {
        List<Prerequisite> prerequisites = new ArrayList<>();
        prerequisites.add(new Prerequisite("not-existent-feature-flag", new HashSet<>(Arrays.asList("on", "off"))));
        PrerequisitesMatcher matcher = new PrerequisitesMatcher(prerequisites);
        
        assertFalse(matcher.match("a-key", null, null, mEvaluator));
    }
    
    @Test
    public void shouldReturnFalseWithEmptyTreatmentsList() {
        List<Prerequisite> prerequisites = new ArrayList<>();
        prerequisites.add(new Prerequisite("always-on", new HashSet<>()));
        PrerequisitesMatcher matcher = new PrerequisitesMatcher(prerequisites);
        
        assertFalse(matcher.match("a-key", null, null, mEvaluator));
    }
}
