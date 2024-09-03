package io.split.android.client.service.sseclient;

import static org.junit.Assert.assertEquals;
import static io.split.android.client.service.sseclient.notifications.NotificationType.MEMBERSHIP_LS_UPDATE;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.split.android.client.common.CompressionType;
import io.split.android.client.service.sseclient.notifications.ControlNotification;
import io.split.android.client.service.sseclient.notifications.HashingAlgorithm;
import io.split.android.client.service.sseclient.notifications.IncomingNotification;
import io.split.android.client.service.sseclient.notifications.MembershipNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentUpdateStrategy;
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

    private static final String MY_LARGE_SEGMENTS_UPDATE = "{\"id\": \"diSrQttrC9:0:0\",\"clientId\": \"pri:MjcyNDE2NDUxMA==\",\"timestamp\": 1702507131100,\"encoding\": \"json\",\"channel\": \"NzM2MDI5Mzc0_MTc1MTYwODQxMQ==_memberships\",\"data\": \"{\\\"type\\\":\\\"MEMBERSHIP_LS_UPDATE\\\",\\\"cn\\\":1702507130121,\\\"n\\\":[\\\"android_test\\\"],\\\"c\\\":2,\\\"u\\\":2,\\\"d\\\":\\\"eJwEwLsRwzAMA9BdWKsg+IFBraJTkRXS5rK7388+tg+KdC8+jq4eBBQLFcUnO8FAAC36gndOSEyFqJFP32Vf2+f+3wAAAP//hUQQ9A==\\\",\\\"i\\\":100,\\\"h\\\":1,\\\"s\\\":325}\"}";

    @Before
    public void setup() {
        mParser = new NotificationParser();
    }

    @Test
    public void processSplitUpdate() {
        IncomingNotification incoming = mParser.parseIncoming(SPLIT_UPDATE_NOTIFICATION);
        SplitsChangeNotification splitUpdate = mParser.parseSplitUpdate(incoming.getJsonData());

        assertEquals(NotificationType.SPLIT_UPDATE, incoming.getType());
        assertEquals(1584554772108L, splitUpdate.getChangeNumber());
    }

    @Test
    public void processSplitKill() {
        IncomingNotification incoming = mParser.parseIncoming(SPLIT_KILL_NOTIFICATION);
        SplitKillNotification splitKill = mParser.parseSplitKill(incoming.getJsonData());

        assertEquals(NotificationType.SPLIT_KILL, incoming.getType());
        assertEquals("dep_split", splitKill.getSplitName());
        assertEquals("off", splitKill.getDefaultTreatment());
    }

    @Test
    public void processMySegmentUpdate() {
        IncomingNotification incoming = mParser.parseIncoming(MY_SEGMENT_UDATE_NOTIFICATION);
        MySegmentChangeNotification mySegmentUpdate = mParser.parseMySegmentUpdate(incoming.getJsonData());

        assertEquals(NotificationType.MY_SEGMENTS_UPDATE, incoming.getType());
        assertEquals(1584647532812L, mySegmentUpdate.getChangeNumber());
        Assert.assertFalse(mySegmentUpdate.isIncludesPayload());
    }

    @Test
    public void processMySegmentUpdateInline() {
        IncomingNotification incoming = mParser.parseIncoming(MY_SEGMENT_UDATE_INLINE_NOTIFICATION);
        MySegmentChangeNotification mySegmentUpdate = mParser.parseMySegmentUpdate(incoming.getJsonData());

        assertEquals(NotificationType.MY_SEGMENTS_UPDATE, incoming.getType());
        assertEquals(1584647532812L, mySegmentUpdate.getChangeNumber());
        Assert.assertTrue(mySegmentUpdate.isIncludesPayload());
        assertEquals(2, mySegmentUpdate.getSegmentList().size());
        assertEquals("segment1", mySegmentUpdate.getSegmentList().get(0));
        assertEquals("segment2", mySegmentUpdate.getSegmentList().get(1));
    }

    @Test
    public void processOccupancy() {
        IncomingNotification incoming = mParser.parseIncoming(OCCUPANCY);

        OccupancyNotification notification = mParser.parseOccupancy(incoming.getJsonData());

        assertEquals(NotificationType.OCCUPANCY, notification.getType());
        assertEquals(1, notification.getMetrics().getPublishers());
    }

    @Test
    public void processControl() {
        IncomingNotification incoming = mParser.parseIncoming(CONTROL);
        ControlNotification notification = mParser.parseControl(incoming.getJsonData());

        assertEquals(NotificationType.CONTROL, notification.getType());
        assertEquals(ControlNotification.ControlType.STREAMING_RESUMED, notification.getControlType());
    }

    @Test
    public void parseErrorMessage() {

        String data  = "{\"message\":\"Token expired\",\"code\":40142,\"statusCode\":401,\"href\":\"https://help.ably.io/error/40142\"}";

        StreamingError errorMsg = mParser.parseError(data);

        assertEquals("Token expired", errorMsg.getMessage());
        assertEquals(40142, errorMsg.getCode());
        assertEquals(401, errorMsg.getStatusCode());
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

    @Test
    public void parseMyLargeSegmentsIncomingNotification() {
        IncomingNotification incoming = mParser.parseIncoming(MY_LARGE_SEGMENTS_UPDATE);

        assertEquals(MEMBERSHIP_LS_UPDATE, incoming.getType());
        assertEquals("{\"type\":\"MEMBERSHIP_LS_UPDATE\",\"cn\":1702507130121,\"n\":[\"android_test\"],\"c\":2,\"u\":2,\"d\":\"eJwEwLsRwzAMA9BdWKsg+IFBraJTkRXS5rK7388+tg+KdC8+jq4eBBQLFcUnO8FAAC36gndOSEyFqJFP32Vf2+f+3wAAAP//hUQQ9A==\",\"i\":100,\"h\":1,\"s\":325}", incoming.getJsonData());
        assertEquals("NzM2MDI5Mzc0_MTc1MTYwODQxMQ==_memberships", incoming.getChannel());
        assertEquals(1702507131100L, incoming.getTimestamp());
    }

    @Test
    public void parseMyLargeSegmentsNotificationData() {
        IncomingNotification incomingNotification = mParser.parseIncoming(MY_LARGE_SEGMENTS_UPDATE);
        MembershipNotification notification = mParser.parseMembershipNotification(incomingNotification.getJsonData());

        assertEquals("eJwEwLsRwzAMA9BdWKsg+IFBraJTkRXS5rK7388+tg+KdC8+jq4eBBQLFcUnO8FAAC36gndOSEyFqJFP32Vf2+f+3wAAAP//hUQQ9A==", notification.getData());
        assertEquals((Long) 1702507130121L, notification.getChangeNumber());
        assertEquals(Collections.singleton("android_test"), notification.getNames());
        assertEquals(CompressionType.ZLIB, notification.getCompression());
        assertEquals(MySegmentUpdateStrategy.KEY_LIST, notification.getUpdateStrategy());
        assertEquals((Long) 100L, notification.getUpdateIntervalMs());
        assertEquals((Integer) 325, notification.getAlgorithmSeed());
        assertEquals(HashingAlgorithm.MURMUR3_32, notification.getHashingAlgorithm());
    }
}
