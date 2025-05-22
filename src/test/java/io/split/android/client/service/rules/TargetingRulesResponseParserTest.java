package io.split.android.client.service.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import io.split.android.client.dtos.Excluded;
import io.split.android.client.dtos.ExcludedSegment;
import io.split.android.client.dtos.Prerequisite;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.TargetingRulesChange;
import io.split.android.client.service.http.HttpResponseParserException;
import io.split.android.helpers.FileHelper;

public class TargetingRulesResponseParserTest {

    private TargetingRulesResponseParser parser;
    private FileHelper fileHelper;

    @Before
    public void setUp() {
        parser = new TargetingRulesResponseParser();
        fileHelper = new FileHelper();
    }

    @Test
    public void parsesNewTargetingRulesChangeJson() throws Exception {
        String json = fileHelper.loadFileContent("split_changes_small.json");
        TargetingRulesChange result = parser.parse(json);
        assertNotNull(result);
        assertNotNull(result.getFeatureFlagsChange());
        assertEquals("FACUNDO_TEST", result.getFeatureFlagsChange().splits.get(0).name);
        assertEquals(1506703262916L, result.getFeatureFlagsChange().till);
        assertEquals(-1, result.getFeatureFlagsChange().since);
        assertEquals(1506703262920L, result.getRuleBasedSegmentsChange().getSince());
        assertEquals(1506703263000L, result.getRuleBasedSegmentsChange().getTill());
        assertEquals("mauro_rule_based_segment", result.getRuleBasedSegmentsChange().getSegments().get(0).getName());
        Excluded excluded = result.getRuleBasedSegmentsChange().getSegments().get(0).getExcluded();
        assertEquals(1, excluded.getKeys().size());
        Set<ExcludedSegment> excludedSegments = excluded.getSegments();
        assertEquals(4, excludedSegments.size());
        // check that it contains 4 excluded segments: standard, large, rule-based and unsupported
        assertTrue(excludedSegments.contains(ExcludedSegment.standard("segment_test")));
        assertTrue(excludedSegments.contains(ExcludedSegment.large("segment_test2")));
        assertTrue(excludedSegments.contains(ExcludedSegment.ruleBased("segment_test3")));
    }

    @Test
    public void parsesLegacySplitChangeJson() throws Exception {
        String json = fileHelper.loadFileContent("split_changes_legacy.json");
        TargetingRulesChange result = parser.parse(json);
        assertNotNull(result);
        assertNotNull(result.getFeatureFlagsChange());
        assertEquals("FACUNDO_TEST", result.getFeatureFlagsChange().splits.get(0).name);
        assertEquals(1506703262916L, result.getFeatureFlagsChange().till);
        assertEquals(-1, result.getFeatureFlagsChange().since);
        assertEquals(-1, result.getRuleBasedSegmentsChange().getSince());
        assertEquals(-1, result.getRuleBasedSegmentsChange().getTill());
        assertTrue(result.getRuleBasedSegmentsChange().getSegments().isEmpty());
    }

    @Test
    public void parsesPrerequisites() throws HttpResponseParserException {
        String json = fileHelper.loadFileContent("split_changes_prerequisites.json");
        TargetingRulesChange result = parser.parse(json);

        assertNotNull(result);
        assertNotNull(result.getFeatureFlagsChange());
        Split firstSplit = result.getFeatureFlagsChange().splits.get(0);
        assertEquals("FACUNDO_TEST", firstSplit.name);
        List<Prerequisite> preReqs = firstSplit.getPrerequisites();
        assertEquals(2, preReqs.size());
        assertEquals("flag1", preReqs.get(0).getName());
        assertEquals("flag2", preReqs.get(1).getName());
        assertEquals(2, preReqs.get(0).getTreatments().size());
        assertEquals(1, preReqs.get(1).getTreatments().size());
        assertTrue(preReqs.get(0).getTreatments().contains("on"));
        assertTrue(preReqs.get(0).getTreatments().contains("v1"));
        assertTrue(preReqs.get(1).getTreatments().contains("off"));
    }

    @Test
    public void nonExistingPrerequisitesDefaultsToEmpty() throws HttpResponseParserException {
        String json = fileHelper.loadFileContent("split_changes_prerequisites.json");
        TargetingRulesChange result = parser.parse(json);
        assertNotNull(result);
        Split split = result.getFeatureFlagsChange().splits.get(1);
        assertEquals("FACUNDO_TEST_2", split.name);
        List<Prerequisite> preReqs = split.getPrerequisites();
        assertEquals(0, preReqs.size());
    }

    @Test
    public void parseNullReturnsNull() throws HttpResponseParserException {
        TargetingRulesChange result = parser.parse(null);
        assertNull(result);
    }

    @Test
    public void parseEmptyReturnsNull() throws HttpResponseParserException {
        TargetingRulesChange result = parser.parse("");
        assertNull(result);
    }
}
