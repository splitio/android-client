package io.split.android.client.service.sseclient;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.notifications.IncomingNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.NotificationType;
import io.split.android.client.service.sseclient.notifications.RawNotification;
import io.split.android.client.service.sseclient.notifications.SplitKillNotification;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;

public class NotificationParserTest {

    NotificationParser mParser;

    private final static String SPLIT_UPDATE_NOTIFICATION =
            "{\"id\":\"VSEQrcq9D8:0:0\",\"clientId\":\"NDEzMTY5Mzg0MA==:MjU4MzkwNDA2NA==\",\"timestamp\":1584554772719,\"encoding\":\"json\",\"channel\":\"MzM5Njc0ODcyNg==_MTExMzgwNjgx_splits\",\n" +
                    "\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":1584554772108}\"}";

    private final static String MY_SEGMENT_UDATE_NOTIFICATION = "{\"id\":\"x2dE2TEiJL:0:0\",\"clientId\":\"NDEzMTY5Mzg0MA==:OTc5Nzc4NDYz\",\"timestamp\":1584647533288,\"encoding\":\"json\",\"channel\":\"MzM5Njc0ODcyNg==_MTExMzgwNjgx_MTcwNTI2MTM0Mg==_mySegments\",\"data\":\"{\\\"type\\\":\\\"MY_SEGMENTS_UPDATE\\\",\\\"changeNumber\\\":1584647532812,\\\"includesPayload\\\":false}\"}";

    private final  static String SPLIT_KILL_NOTIFICATION = "{\"id\":\"-OT-rGuSwz:0:0\",\"clientId\":\"NDEzMTY5Mzg0MA==:NDIxNjU0NTUyNw==\",\"timestamp\":1584647606489,\"encoding\":\"json\",\"channel\":\"MzM5Njc0ODcyNg==_MTExMzgwNjgx_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_KILL\\\",\\\"changeNumber\\\":1584647606125,\\\"defaultTreatment\\\":\\\"off\\\",\\\"splitName\\\":\\\"dep_split\\\"}\"}";

    private final static String MY_SEGMENT_UDATE_INLINE_NOTIFICATION = "{\"id\":\"x2dE2TEiJL:0:0\",\"clientId\":\"NDEzMTY5Mzg0MA==:OTc5Nzc4NDYz\",\"timestamp\":1584647533288,\"encoding\":\"json\",\"channel\":\"MzM5Njc0ODcyNg==_MTExMzgwNjgx_MTcwNTI2MTM0Mg==_mySegments\",\"data\":\"{\\\"type\\\":\\\"MY_SEGMENTS_UPDATE\\\",\\\"changeNumber\\\":1584647532812,\\\"includesPayload\\\":true,\\\"segmentList\\\":[\\\"segment1\\\", \\\"segment2\\\"]}\"}";

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
}
