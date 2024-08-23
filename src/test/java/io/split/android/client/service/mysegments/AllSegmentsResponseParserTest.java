package io.split.android.client.service.mysegments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.dtos.AllSegmentsChange;
import io.split.android.client.service.http.HttpResponseParserException;

public class AllSegmentsResponseParserTest {

    private AllSegmentsResponseParser mParser;

    @Before
    public void setUp() {
        mParser = new AllSegmentsResponseParser();
    }

    @Test
    public void parse() throws HttpResponseParserException {
        String response = "{\"ms\":{\"k\":[{\"n\":\"segment1\"},{\"n\":\"segment2\"}], \"cn\":null},\"ls\":{\"k\":[{\"n\":\"large-segment1\"},{\"n\":\"large-segment2\"},{\"n\":\"large-segment3\"}], \"cn\":2000}}";

        AllSegmentsChange parsed = mParser.parse(response);

        assertTrue(parsed.getSegmentsChange().getNames().contains("segment1"));
        assertTrue(parsed.getSegmentsChange().getNames().contains("segment2"));
        assertTrue(parsed.getLargeSegmentsChange().getNames().contains("large-segment1"));
        assertTrue(parsed.getLargeSegmentsChange().getNames().contains("large-segment2"));
        assertTrue(parsed.getLargeSegmentsChange().getNames().contains("large-segment3"));
        assertEquals(2, parsed.getSegmentsChange().getSegments().size());
        assertEquals(3, parsed.getLargeSegmentsChange().getSegments().size());
        assertNull(parsed.getSegmentsChange().getChangeNumber());
        assertEquals(Long.valueOf(2000), parsed.getLargeSegmentsChange().getChangeNumber());
    }
}
