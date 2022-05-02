package io.split.android.client.service.sseclient;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.service.sseclient.notifications.ControlNotification;
import io.split.android.client.service.sseclient.notifications.IncomingNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationType;
import io.split.android.client.service.sseclient.notifications.OccupancyNotification;
import io.split.android.client.service.sseclient.notifications.SplitKillNotification;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.service.sseclient.notifications.StreamingError;

public class NotificationParserTest {

    NotificationParser mParser;

    private final static String SPLIT_UPDATE_NOTIFICATION =
            "{\"id\":\"VSEQrcq9D8:0:0\",\"clientId\":\"NDEzMTY5Mzg0MA==:MjU4MzkwNDA2NA==\",\"timestamp\":1584554772719,\"encoding\":\"json\",\"channel\":\"MzM5Njc0ODcyNg==_MTExMzgwNjgx_splits\",\n" +
                    "\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1584554772108}\"}";

    private final static String MY_SEGMENT_UDATE_NOTIFICATION = "{\"id\":\"x2dE2TEiJL:0:0\",\"clientId\":\"NDEzMTY5Mzg0MA==:OTc5Nzc4NDYz\",\"timestamp\":1584647533288,\"encoding\":\"json\",\"channel\":\"MzM5Njc0ODcyNg==_MTExMzgwNjgx_MTcwNTI2MTM0Mg==_mySegments\",\"data\":\"{\\\"type\\\":\\\"MY_SEGMENTS_UPDATE\\\",\\\"changeNumber\\\":1584647532812,\\\"includesPayload\\\":false}\"}";

    private final static String SPLIT_KILL_NOTIFICATION = "{\"id\":\"-OT-rGuSwz:0:0\",\"clientId\":\"NDEzMTY5Mzg0MA==:NDIxNjU0NTUyNw==\",\"timestamp\":1584647606489,\"encoding\":\"json\",\"channel\":\"MzM5Njc0ODcyNg==_MTExMzgwNjgx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_KILL\\\",\\\"changeNumber\\\":1584647606125,\\\"defaultTreatment\\\":\\\"off\\\",\\\"splitName\\\":\\\"dep_split\\\"}\"}";

    private final static String MY_SEGMENT_UDATE_INLINE_NOTIFICATION = "{\"id\":\"x2dE2TEiJL:0:0\",\"clientId\":\"NDEzMTY5Mzg0MA==:OTc5Nzc4NDYz\",\"timestamp\":1584647533288,\"encoding\":\"json\",\"channel\":\"MzM5Njc0ODcyNg==_MTExMzgwNjgx_MTcwNTI2MTM0Mg==_mySegments\",\"data\":\"{\\\"type\\\":\\\"MY_SEGMENTS_UPDATE\\\",\\\"changeNumber\\\":1584647532812,\\\"includesPayload\\\":true,\\\"segmentList\\\":[\\\"segment1\\\", \\\"segment2\\\"]}\"}";

    private final static String OCCUPANCY = "{\"id\":\"x2dE2TEiJL:0:0\",\"clientId\":\"NDEzMTY5Mzg0MA==:OTc5Nzc4NDYz\",\"timestamp\":1584647533288,\"encoding\":\"json\",\"channel\":\"control_pri\",\"data\":\"{\\\"metrics\\\": {\\\"publishers\\\":1}}\"}";

    private final static String CONTROL = "{\"id\":\"x2dE2TEiJL:0:0\",\"clientId\":\"NDEzMTY5Mzg0MA==:OTc5Nzc4NDYz\",\"timestamp\":1584647533288,\"encoding\":\"json\",\"channel\":\"control_pri\",\"data\":\"{\\\"type\\\":\\\"CONTROL\\\",\\\"controlType\\\":\\\"STREAMING_RESUMED\\\"}\"}";

    private final static String ERROR = "{\"id\":\"null\",\"name\":\"error\",\"comment\":\"[no comments]\",\"data\":\"{\\\"message\\\":\\\"Invalid token; capability must be a string\\\",\\\"code\\\":40144,\\\"statusCode\\\":400,\\\"href\\\":\\\"https://help.ably.io/error/40144\\\"}\"}";

    @Before
    public void setup() {
        mParser = new NotificationParser();
    }

    @Test
    public void processSplitUpdate() {
        IncomingNotification incoming = mParser.parseIncoming(SPLIT_UPDATE_NOTIFICATION);
        SplitsChangeNotification splitUpdate = mParser.parseSplitUpdate(incoming.getJsonData());

        Assert.assertEquals(NotificationType.SPLIT_UPDATE, incoming.getType());
        Assert.assertEquals(1584554772108L, splitUpdate.getChangeNumber());
    }

    @Test
    public void processSplitKill() {
        IncomingNotification incoming = mParser.parseIncoming(SPLIT_KILL_NOTIFICATION);
        SplitKillNotification splitKill = mParser.parseSplitKill(incoming.getJsonData());

        Assert.assertEquals(NotificationType.SPLIT_KILL, incoming.getType());
        Assert.assertEquals("dep_split", splitKill.getSplitName());
        Assert.assertEquals("off", splitKill.getDefaultTreatment());
    }

    @Test
    public void processMySegmentUpdate() {
        IncomingNotification incoming = mParser.parseIncoming(MY_SEGMENT_UDATE_NOTIFICATION);
        MySegmentChangeNotification mySegmentUpdate = mParser.parseMySegmentUpdate(incoming.getJsonData());

        Assert.assertEquals(NotificationType.MY_SEGMENTS_UPDATE, incoming.getType());
        Assert.assertEquals(1584647532812L, mySegmentUpdate.getChangeNumber());
        Assert.assertFalse(mySegmentUpdate.isIncludesPayload());
    }

    @Test
    public void processMySegmentUpdateInline() {
        IncomingNotification incoming = mParser.parseIncoming(MY_SEGMENT_UDATE_INLINE_NOTIFICATION);
        MySegmentChangeNotification mySegmentUpdate = mParser.parseMySegmentUpdate(incoming.getJsonData());

        Assert.assertEquals(NotificationType.MY_SEGMENTS_UPDATE, incoming.getType());
        Assert.assertEquals(1584647532812L, mySegmentUpdate.getChangeNumber());
        Assert.assertTrue(mySegmentUpdate.isIncludesPayload());
        Assert.assertEquals(2, mySegmentUpdate.getSegmentList().size());
        Assert.assertEquals("segment1", mySegmentUpdate.getSegmentList().get(0));
        Assert.assertEquals("segment2", mySegmentUpdate.getSegmentList().get(1));
    }

    @Test
    public void processOccupancy() {
        IncomingNotification incoming = mParser.parseIncoming(OCCUPANCY);

        OccupancyNotification notification = mParser.parseOccupancy(incoming.getJsonData());

        Assert.assertEquals(NotificationType.OCCUPANCY, notification.getType());
        Assert.assertEquals(1, notification.getMetrics().getPublishers());
    }

    @Test
    public void processControl() {
        IncomingNotification incoming = mParser.parseIncoming(CONTROL);
        ControlNotification notification = mParser.parseControl(incoming.getJsonData());

        Assert.assertEquals(NotificationType.CONTROL, notification.getType());
        Assert.assertEquals(ControlNotification.ControlType.STREAMING_RESUMED, notification.getControlType());
    }

    @Test
    public void parseErrorMessage() {

        String data  = "{\"message\":\"Token expired\",\"code\":40142,\"statusCode\":401,\"href\":\"https://help.ably.io/error/40142\"}";

        StreamingError errorMsg = mParser.parseError(data);

        Assert.assertEquals("Token expired", errorMsg.getMessage());
        Assert.assertEquals(40142, errorMsg.getCode());
        Assert.assertEquals(401, errorMsg.getStatusCode());
    }

    @Test
    public void isError() {

        // Check is event is error
        Map<String, String> event = new HashMap<>();
        event.put("event", "error");
        event.put("data", "{{{\"message\":\"Token expired\",\"code\":40142,\"statusCode\":401,\"href\":\"https://help.ably.io/error/40142\"}");

        boolean isError = mParser.isError(event);

        Assert.assertTrue(isError);
    }

    @Test
    public void isNotError() {

        // Check is event is error
        Map<String, String> event = new HashMap<>();
        event.put("event", "noerror");
        event.put("data", "{{{\"message\":\"Token expired\",\"code\":40142,\"statusCode\":401,\"href\":\"https://help.ably.io/error/40142\"}");

        boolean isError = mParser.isError(event);

        Assert.assertFalse(isError);
    }

    @Test
    public void NoCrashIfisNullEventError() {

        // Check if no crashing when null values are passed to the function
        Map<String, String> event = new HashMap<>();
        event.put("event", "noerror");
        event.put("data", "{{{\"message\":\"Token expired\",\"code\":40142,\"statusCode\":401,\"href\":\"https://help.ably.io/error/40142\"}");

        boolean isError = mParser.isError(null);

        Assert.assertFalse(isError);
    }
}
