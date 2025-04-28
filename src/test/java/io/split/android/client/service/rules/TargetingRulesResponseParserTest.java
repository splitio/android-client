package io.split.android.client.service.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

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
