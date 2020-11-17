package io.split.android.client.service.sseclient;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class EventStreamParserTest {
    EventStreamParser mParser;
    Map<String, String> mValues;

    @Before
    public void setup() {
        mParser = new EventStreamParser();
        mValues = new HashMap<>();
    }

    @Test
    public void parseNormalLine() {
        boolean res = mParser.parseLineAndAppendValue("id:theid", mValues);

        Assert.assertFalse(res);
        Assert.assertEquals(1, mValues.size());
        Assert.assertEquals(mValues.get("id"), "theid");
    }

    @Test
    public void parseColon() {
        boolean res = mParser.parseLineAndAppendValue(":", mValues);

        Assert.assertFalse(res);
        Assert.assertEquals(0, mValues.size());
    }

    @Test
    public void parseEmptyLineNoEnd() {
        boolean res = mParser.parseLineAndAppendValue("", mValues);

        Assert.assertFalse(res);
        Assert.assertEquals(0, mValues.size());
    }

    @Test
    public void parseEnd() {
        boolean res0 = mParser.parseLineAndAppendValue("id:theid", mValues);
        boolean res1 = mParser.parseLineAndAppendValue("event:message", mValues);
        boolean res2 = mParser.parseLineAndAppendValue("data:{\"c1\":1}", mValues);
        boolean res = mParser.parseLineAndAppendValue("", mValues);

        Assert.assertFalse(res0);
        Assert.assertFalse(res1);
        Assert.assertFalse(res2);
        Assert.assertTrue(res);
        Assert.assertEquals(3, mValues.size());
        Assert.assertEquals("theid", mValues.get("id"));
        Assert.assertEquals("message", mValues.get("event"));
        Assert.assertEquals("{\"c1\":1}", mValues.get("data"));
    }

    @Test
    public void parseTwoColon() {
        boolean res = mParser.parseLineAndAppendValue("id:value:value", mValues);

        Assert.assertFalse(res);
        Assert.assertEquals(1, mValues.size());
        Assert.assertEquals("value:value", mValues.get("id"));
    }

    @Test
    public void parseNoColon() {
        boolean res = mParser.parseLineAndAppendValue("fieldName", mValues);

        Assert.assertFalse(res);
        Assert.assertEquals(1, mValues.size());
        Assert.assertEquals("", mValues.get("fieldName"));
    }

    @Test
    public void parseNoFieldName() {
        boolean res = mParser.parseLineAndAppendValue(":fieldName", mValues);

        Assert.assertFalse(res);
        Assert.assertEquals(0, mValues.size());
    }

    @Test
    public void parseNull() {
        boolean res = mParser.parseLineAndAppendValue(null, mValues);

        Assert.assertFalse(res);
        Assert.assertEquals(0, mValues.size());
    }


}
