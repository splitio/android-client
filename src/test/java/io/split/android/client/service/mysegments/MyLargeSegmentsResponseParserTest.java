package io.split.android.client.service.mysegments;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import io.split.android.client.dtos.MyLargeSegmentsResponse;
import io.split.android.client.service.http.HttpResponseParserException;

public class MyLargeSegmentsResponseParserTest {

    private MyLargeSegmentsResponseParser mParser;

    @Before
    public void setUp() {
        mParser = new MyLargeSegmentsResponseParser();
    }

    @Test
    public void parse() throws HttpResponseParserException {
        String response = "{\"myLargeSegments\":[\"segment1\",\"segment2\"], \"till\":2000}";

        MyLargeSegmentsResponse parse = mParser.parse(response);

        List<String> segments = parse.getSegments();
        assertEquals(2, segments.size());
        assertEquals("segment1", segments.get(0));
        assertEquals("segment2", segments.get(1));
        assertEquals(2000, parse.getTill());
    }
}
