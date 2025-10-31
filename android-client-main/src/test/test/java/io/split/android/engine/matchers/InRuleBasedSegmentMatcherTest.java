package io.split.android.engine.matchers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.Evaluator;
import io.split.android.client.dtos.ExcludedSegment;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.engine.experiments.ParsedCondition;
import io.split.android.engine.experiments.ParsedRuleBasedSegment;

public class InRuleBasedSegmentMatcherTest {

    private static final String SEGMENT_NAME = "test-segment";
    private static final String MATCHING_KEY = "test-key";
    private static final String BUCKETING_KEY = "test-bucketing-key";

    private RuleBasedSegmentStorage mRuleBasedSegmentStorage;
    private MySegmentsStorage mMySegmentsStorage;
    private MySegmentsStorage mMyLargeSegmentsStorage;
    private InRuleBasedSegmentMatcher mMatcher;
    private Evaluator mEvaluator;

    @Before
    public void setUp() {
        mRuleBasedSegmentStorage = mock(RuleBasedSegmentStorage.class);
        mMySegmentsStorage = mock(MySegmentsStorage.class);
        mMyLargeSegmentsStorage = mock(MySegmentsStorage.class);
        mEvaluator = mock(Evaluator.class);
        mMatcher = new InRuleBasedSegmentMatcher(mRuleBasedSegmentStorage, mMySegmentsStorage, mMyLargeSegmentsStorage, SEGMENT_NAME);
    }

    @Test
    public void matchReturnsFalseWhenMatchValueIsNotString() {
        assertFalse(mMatcher.match(123, BUCKETING_KEY, Collections.emptyMap(), mEvaluator));
        assertFalse(mMatcher.match(true, BUCKETING_KEY, Collections.emptyMap(), mEvaluator));
        assertFalse(mMatcher.match(null, BUCKETING_KEY, Collections.emptyMap(), mEvaluator));
    }

    @Test
    public void matchReturnsFalseWhenKeyIsExcluded() {
        ParsedRuleBasedSegment segment = createSegment(
                new HashSet<>(Collections.singletonList(MATCHING_KEY)),
                Collections.emptySet(),
                Collections.emptyList()
        );

        when(mRuleBasedSegmentStorage.get(eq(SEGMENT_NAME), eq(MATCHING_KEY))).thenReturn(segment);

        assertFalse(mMatcher.match(MATCHING_KEY, BUCKETING_KEY, Collections.emptyMap(), mEvaluator));
    }

    @Test
    public void matchReturnsFalseWhenInExcludedSegment() {
        ExcludedSegment excludedSegment = ExcludedSegment.standard("excluded-segment");
        Set<String> mySegments = new HashSet<>(Collections.singletonList("excluded-segment"));

        ParsedRuleBasedSegment segment = createSegment(
                Collections.emptySet(),
                new HashSet<>(Collections.singletonList(excludedSegment)),
                Collections.emptyList()
        );

        when(mRuleBasedSegmentStorage.get(eq(SEGMENT_NAME), eq(MATCHING_KEY))).thenReturn(segment);
        when(mMySegmentsStorage.getAll()).thenReturn(mySegments);

        assertFalse(mMatcher.match(MATCHING_KEY, BUCKETING_KEY, Collections.emptyMap(), mEvaluator));
    }

    @Test
    public void matchReturnsFalseWhenInExcludedLargeSegment() {
        ExcludedSegment excludedSegment = ExcludedSegment.large("excluded-segment");
        Set<String> mySegments = new HashSet<>(Collections.singletonList("excluded-segment"));

        ParsedRuleBasedSegment segment = createSegment(
                Collections.emptySet(),
                new HashSet<>(Collections.singletonList(excludedSegment)),
                Collections.emptyList()
        );

        when(mRuleBasedSegmentStorage.get(eq(SEGMENT_NAME), eq(MATCHING_KEY))).thenReturn(segment);
        when(mMyLargeSegmentsStorage.getAll()).thenReturn(mySegments);

        assertFalse(mMatcher.match(MATCHING_KEY, BUCKETING_KEY, Collections.emptyMap(), mEvaluator));
    }

    @Test
    public void matchReturnsFalseWhenInExcludedRuleBasedSegment() {
        ExcludedSegment excludedSegment = ExcludedSegment.ruleBased("excluded-segment");

        ParsedRuleBasedSegment segment = createSegment(
                Collections.emptySet(),
                new HashSet<>(Collections.singletonList(excludedSegment)),
                Collections.emptyList()
        );
        CombiningMatcher conditionMatcher = mock(CombiningMatcher.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("age", 30);
        attributes.put("country", "US");

        when(conditionMatcher.match(eq(MATCHING_KEY), eq(BUCKETING_KEY), eq(attributes), eq(mEvaluator))).thenReturn(true);

        ParsedCondition condition = mock(ParsedCondition.class);
        when(condition.matcher()).thenReturn(conditionMatcher);

        List<ParsedCondition> conditions = Collections.singletonList(condition);

        ParsedRuleBasedSegment storedExcludedRbs
                = createSegment(Collections.emptySet(), Collections.emptySet(), conditions);

        when(mRuleBasedSegmentStorage.get(eq(SEGMENT_NAME), eq(MATCHING_KEY))).thenReturn(segment);
        when(mRuleBasedSegmentStorage.get(eq("excluded-segment"), eq(MATCHING_KEY))).thenReturn(storedExcludedRbs);

        assertFalse(mMatcher.match(MATCHING_KEY, BUCKETING_KEY, attributes, mEvaluator));
    }

    @Test
    public void matchReturnsTrueWhenConditionMatches() {
        CombiningMatcher conditionMatcher = mock(CombiningMatcher.class);
        when(conditionMatcher.match(eq(MATCHING_KEY), eq(BUCKETING_KEY), anyMap(), eq(mEvaluator))).thenReturn(true);

        ParsedCondition condition = mock(ParsedCondition.class);
        when(condition.matcher()).thenReturn(conditionMatcher);

        List<ParsedCondition> conditions = Collections.singletonList(condition);

        ParsedRuleBasedSegment segment = createSegment(
                Collections.emptySet(),
                Collections.emptySet(),
                conditions
        );

        when(mRuleBasedSegmentStorage.get(eq(SEGMENT_NAME), eq(MATCHING_KEY))).thenReturn(segment);

        assertTrue(mMatcher.match(MATCHING_KEY, BUCKETING_KEY, Collections.emptyMap(), mEvaluator));
    }

    @Test
    public void matchReturnsFalseWhenNoConditionMatches() {
        CombiningMatcher conditionMatcher = mock(CombiningMatcher.class);
        when(conditionMatcher.match(eq(MATCHING_KEY), eq(BUCKETING_KEY), anyMap(), eq(mEvaluator))).thenReturn(false);

        ParsedCondition condition = mock(ParsedCondition.class);
        when(condition.matcher()).thenReturn(conditionMatcher);

        List<ParsedCondition> conditions = Collections.singletonList(condition);

        ParsedRuleBasedSegment segment = createSegment(
                Collections.emptySet(),
                Collections.emptySet(),
                conditions
        );

        when(mRuleBasedSegmentStorage.get(eq(SEGMENT_NAME), eq(MATCHING_KEY))).thenReturn(segment);

        assertFalse(mMatcher.match(MATCHING_KEY, BUCKETING_KEY, Collections.emptyMap(), mEvaluator));
    }

    @Test
    public void matchReturnsTrueWhenOneOfMultipleConditionsMatches() {
        CombiningMatcher conditionMatcher1 = mock(CombiningMatcher.class);
        when(conditionMatcher1.match(eq(MATCHING_KEY), eq(BUCKETING_KEY), anyMap(), eq(mEvaluator))).thenReturn(false);

        CombiningMatcher conditionMatcher2 = mock(CombiningMatcher.class);
        when(conditionMatcher2.match(eq(MATCHING_KEY), eq(BUCKETING_KEY), anyMap(), eq(mEvaluator))).thenReturn(true);

        ParsedCondition condition1 = mock(ParsedCondition.class);
        when(condition1.matcher()).thenReturn(conditionMatcher1);

        ParsedCondition condition2 = mock(ParsedCondition.class);
        when(condition2.matcher()).thenReturn(conditionMatcher2);

        List<ParsedCondition> conditions = Arrays.asList(condition1, condition2);

        ParsedRuleBasedSegment segment = createSegment(
                Collections.emptySet(),
                Collections.emptySet(),
                conditions
        );

        when(mRuleBasedSegmentStorage.get(eq(SEGMENT_NAME), eq(MATCHING_KEY))).thenReturn(segment);

        assertTrue(mMatcher.match(MATCHING_KEY, BUCKETING_KEY, Collections.emptyMap(), mEvaluator));
    }

    @Test
    public void matchWithAttributes() {
        CombiningMatcher conditionMatcher = mock(CombiningMatcher.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("age", 30);
        attributes.put("country", "US");

        when(conditionMatcher.match(eq(MATCHING_KEY), eq(BUCKETING_KEY), eq(attributes), eq(mEvaluator))).thenReturn(true);

        ParsedCondition condition = mock(ParsedCondition.class);
        when(condition.matcher()).thenReturn(conditionMatcher);

        List<ParsedCondition> conditions = Collections.singletonList(condition);

        ParsedRuleBasedSegment segment = createSegment(
                Collections.emptySet(),
                Collections.emptySet(),
                conditions
        );

        when(mRuleBasedSegmentStorage.get(eq(SEGMENT_NAME), eq(MATCHING_KEY))).thenReturn(segment);

        assertTrue(mMatcher.match(MATCHING_KEY, BUCKETING_KEY, attributes, mEvaluator));
    }

    @Test
    public void matchWhenStorageReturnsNull() {
        when(mRuleBasedSegmentStorage.get(eq(SEGMENT_NAME), eq(MATCHING_KEY))).thenReturn(null);

        boolean result = mMatcher.match(MATCHING_KEY, BUCKETING_KEY, Collections.emptyMap(), mEvaluator);
        assertFalse(result);
    }

    private ParsedRuleBasedSegment createSegment(Set<String> excludedKeys, Set<ExcludedSegment> excludedSegments, List<ParsedCondition> conditions) {
        ParsedRuleBasedSegment segment = mock(ParsedRuleBasedSegment.class);
        when(segment.getExcludedKeys()).thenReturn(excludedKeys);
        when(segment.getExcludedSegments()).thenReturn(excludedSegments);
        when(segment.getParsedConditions()).thenReturn(conditions != null ? conditions : new ArrayList<>());
        return segment;
    }
}
