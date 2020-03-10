package io.split.android.http;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.network.sseclient.NotificationParser;

public class NotificationParserTest {
    NotificationParser mParser;
    Map<String, String> mValues;

    @Before
    public void setup() {
        mParser = new NotificationParser();
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
    public void parseEnd() {
        boolean res = mParser.parseLineAndAppendValue("", mValues);

        Assert.assertTrue(res);
        Assert.assertEquals(0, mValues.size());
    }

    @Test
    public void parseTwoColon() {
        boolean res = mParser.parseLineAndAppendValue("id:value:value", mValues);

        Assert.assertFalse(res);
        Assert.assertEquals(1, mValues.size());
        Assert.assertEquals(mValues.get("id"), "value:value");
    }

    @Test
    public void parseNoColon() {
        boolean res = mParser.parseLineAndAppendValue("fieldName", mValues);

        Assert.assertFalse(res);
        Assert.assertEquals(1, mValues.size());
        Assert.assertEquals(mValues.get("fieldName"), "");
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
