package io.split.android.client.service.sseclient;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.service.sseclient.notifications.StreamingError;
import io.split.android.client.service.sseclient.notifications.StreamingMessageParser;

public class StreamingMessageParserTest {

    StreamingMessageParser mMessageParser;

    @Before
    public void setup() {
        mMessageParser = new StreamingMessageParser();
    }

    @Test
    public void parseErrorMessage() {

        // Correct error event. Should match valuesO
        Map<String, String> event = new HashMap<>();
        event.put("event", "error");
        event.put("data", "{\"message\":\"Token expired\",\"code\":40142,\"statusCode\":401,\"href\":\"https://help.ably.io/error/40142\"}");

        StreamingError errorMsg = mMessageParser.parseError(event);

        Assert.assertEquals("Token expired", errorMsg.getMessage());
        Assert.assertEquals(40142, errorMsg.getCode());
        Assert.assertEquals(401, errorMsg.getStatusCode());
    }

    @Test
    public void parseOtherStreamingMessage() {

        // For now non streaming message should be null
        Map<String, String> event = new HashMap<>();
        event.put("event", "message");
        event.put("data", "{\"message\":\"Token expired\",\"code\":40142,\"statusCode\":401,\"href\":\"https://help.ably.io/error/40142\"}");

        StreamingError msg = mMessageParser.parseError(event);

        Assert.assertNull(msg);
    }

    @Test
    public void parseBadSyntaxErrorStreamingMessage() {

        // For wrong json should be null too
        Map<String, String> event = new HashMap<>();
        event.put("event", "error");
        event.put("data", "{{{\"message\":\"Token expired\",\"code\":40142,\"statusCode\":401,\"href\":\"https://help.ably.io/error/40142\"}");

        StreamingError msg = mMessageParser.parseError(event);

        Assert.assertNull(msg);
    }

    @Test
    public void isError() {

        // Check is event is error
        Map<String, String> event = new HashMap<>();
        event.put("event", "error");
        event.put("data", "{{{\"message\":\"Token expired\",\"code\":40142,\"statusCode\":401,\"href\":\"https://help.ably.io/error/40142\"}");

        boolean isError = mMessageParser.isError(event);

        Assert.assertTrue(isError);
    }

    @Test
    public void isNotError() {

        // Check is event is error
        Map<String, String> event = new HashMap<>();
        event.put("event", "noerror");
        event.put("data", "{{{\"message\":\"Token expired\",\"code\":40142,\"statusCode\":401,\"href\":\"https://help.ably.io/error/40142\"}");

        boolean isError = mMessageParser.isError(event);

        Assert.assertFalse(isError);
    }

    @Test
    public void NoCrashIfisNullEventError() {

        // Check if no crashing when null values are passed to the function
        Map<String, String> event = new HashMap<>();
        event.put("event", "noerror");
        event.put("data", "{{{\"message\":\"Token expired\",\"code\":40142,\"statusCode\":401,\"href\":\"https://help.ably.io/error/40142\"}");

        boolean isError = mMessageParser.isError(null);

        Assert.assertFalse(isError);
    }
}
