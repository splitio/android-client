package io.split.android.client.service.mysegments;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.service.http.HttpResponseParserException;

public class MyLargeSegmentsResponseParserTest {

    private MyLargeSegmentsResponseParser mParser;

    @Before
    public void setUp() {
        mParser = new MyLargeSegmentsResponseParser();
    }

    @Test
    public void parse() throws HttpResponseParserException {
        String response = "{\"myLargeSegments\":[\"segment1\",\"segment2\"], \"changeNumber\":2000}";

        List<MySegment> parse = mParser.parse(response);

        assertEquals(2, parse.size());
        assertEquals("segment1", parse.get(0).name);
        assertEquals("segment2", parse.get(1).name);
    }
}
