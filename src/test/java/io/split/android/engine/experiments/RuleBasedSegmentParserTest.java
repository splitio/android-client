package io.split.android.engine.experiments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.split.android.client.dtos.Condition;
import io.split.android.client.dtos.Excluded;
import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.dtos.Status;

public class RuleBasedSegmentParserTest {

    private static final String SEGMENT_NAME = "test-segment";
    private static final String TRAFFIC_TYPE = "user";
    private static final long CHANGE_NUMBER = 123456789L;
    private static final String MATCHING_KEY = "test-key";

    private ParserCommons mParserCommons;
    private RuleBasedSegmentParser mParser;

    @Before
    public void setUp() {
        mParserCommons = mock(ParserCommons.class);
        mParser = new RuleBasedSegmentParser(mParserCommons);
    }

    @Test
    public void validParsing() {
        List<Condition> conditions = new ArrayList<>();
        List<ParsedCondition> parsedConditions = new ArrayList<>();
        parsedConditions.add(mock(ParsedCondition.class));

        Set<String> excludedKeys = new HashSet<>(Arrays.asList("excluded1", "excluded2"));
        Set<String> excludedSegments = new HashSet<>(Arrays.asList("segment1", "segment2"));

        Excluded excluded = mock(Excluded.class);
        when(excluded.getKeys()).thenReturn(excludedKeys);
        when(excluded.getSegments()).thenReturn(excludedSegments);

        RuleBasedSegment segment = new RuleBasedSegment(
                SEGMENT_NAME,
                TRAFFIC_TYPE,
                CHANGE_NUMBER,
                Status.ACTIVE,
                conditions,
                excluded
        );

        when(mParserCommons.getParsedConditions(
                eq(MATCHING_KEY),
                eq(conditions),
                anyString()
        )).thenReturn(parsedConditions);

        ParsedRuleBasedSegment result = mParser.parse(segment, MATCHING_KEY);

        assertNotNull(result);
        assertEquals(SEGMENT_NAME, result.getName());
        assertEquals(TRAFFIC_TYPE, result.getTrafficTypeName());
        assertEquals(CHANGE_NUMBER, result.getChangeNumber());
        assertEquals(excludedKeys, result.getExcludedKeys());
        assertEquals(excludedSegments, result.getExcludedSegments());
        assertEquals(parsedConditions, result.getParsedConditions());
    }

    @Test
    public void parseWithNullConditionsCreatesEmptyConditionList() {
        List<Condition> conditions = new ArrayList<>();

        Excluded excluded = mock(Excluded.class);
        when(excluded.getKeys()).thenReturn(Collections.emptySet());
        when(excluded.getSegments()).thenReturn(Collections.emptySet());

        RuleBasedSegment segment = new RuleBasedSegment(
                SEGMENT_NAME,
                TRAFFIC_TYPE,
                CHANGE_NUMBER,
                Status.ACTIVE,
                conditions,
                excluded
        );

        when(mParserCommons.getParsedConditions(
                eq(MATCHING_KEY),
                eq(conditions),
                anyString()
        )).thenReturn(null);

        ParsedRuleBasedSegment result = mParser.parse(segment, MATCHING_KEY);

        assertNotNull(result);
        assertEquals(SEGMENT_NAME, result.getName());
        assertEquals(Collections.emptyList(), result.getParsedConditions());
    }

    @Test
    public void parseWithNullMatchingKey() {
        List<Condition> conditions = new ArrayList<>();
        List<ParsedCondition> parsedConditions = new ArrayList<>();
        parsedConditions.add(mock(ParsedCondition.class));

        Excluded excluded = mock(Excluded.class);
        when(excluded.getKeys()).thenReturn(Collections.emptySet());
        when(excluded.getSegments()).thenReturn(Collections.emptySet());

        RuleBasedSegment segment = new RuleBasedSegment(
                SEGMENT_NAME,
                TRAFFIC_TYPE,
                CHANGE_NUMBER,
                Status.ACTIVE,
                conditions,
                excluded
        );

        when(mParserCommons.getParsedConditions(
                eq(null),
                eq(conditions),
                anyString()
        )).thenReturn(parsedConditions);

        ParsedRuleBasedSegment result = mParser.parse(segment, null);

        assertNotNull(result);
        assertEquals(parsedConditions, result.getParsedConditions());
    }

    @Test
    public void parseWithNullExcludedReturnsEmptyExcludedLists() {
        List<Condition> conditions = new ArrayList<>();
        List<ParsedCondition> parsedConditions = new ArrayList<>();
        parsedConditions.add(mock(ParsedCondition.class));

        RuleBasedSegment segment = new RuleBasedSegment(
                SEGMENT_NAME,
                TRAFFIC_TYPE,
                CHANGE_NUMBER,
                Status.ACTIVE,
                conditions,
                null
        );

        when(mParserCommons.getParsedConditions(
                eq(MATCHING_KEY),
                eq(conditions),
                anyString()
        )).thenReturn(parsedConditions);

        ParsedRuleBasedSegment result = mParser.parse(segment, MATCHING_KEY);

        assertNotNull(result);
        assertTrue(result.getExcludedKeys().isEmpty());
        assertTrue(result.getExcludedSegments().isEmpty());
    }

    @Test
    public void parseEmptyConditions() {
        List<Condition> conditions = Collections.emptyList();
        List<ParsedCondition> parsedConditions = Collections.emptyList();

        Excluded excluded = mock(Excluded.class);
        when(excluded.getKeys()).thenReturn(Collections.emptySet());
        when(excluded.getSegments()).thenReturn(Collections.emptySet());

        RuleBasedSegment segment = new RuleBasedSegment(
                SEGMENT_NAME,
                TRAFFIC_TYPE,
                CHANGE_NUMBER,
                Status.ACTIVE,
                conditions,
                excluded
        );

        when(mParserCommons.getParsedConditions(
                eq(MATCHING_KEY),
                eq(conditions),
                anyString()
        )).thenReturn(parsedConditions);

        ParsedRuleBasedSegment result = mParser.parse(segment, MATCHING_KEY);

        assertNotNull(result);
        assertEquals(Collections.emptyList(), result.getParsedConditions());
    }
}
