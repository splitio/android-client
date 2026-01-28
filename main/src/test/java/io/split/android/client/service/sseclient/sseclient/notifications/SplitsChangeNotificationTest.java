package io.split.android.client.service.sseclient.sseclient.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.split.android.client.common.CompressionType;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.utils.Json;

public class SplitsChangeNotificationTest {

    private static final String FULL_NOTIFICATION_C0 = "{\"type\":\"SPLIT_UPDATE\",\"changeNumber\":1684265694505,\"pcn\":0,\"c\":0,\"d\":\"redacted=\"}";
    private static final String FULL_NOTIFICATION_C1 = "{\"type\":\"SPLIT_UPDATE\",\"changeNumber\":1684265694505,\"pcn\":0,\"c\":1,\"d\":\"redacted=\"}";
    private static final String FULL_NOTIFICATION_C2 = "{\"type\":\"SPLIT_UPDATE\",\"changeNumber\":1684265694505,\"pcn\":0,\"c\":2,\"d\":\"redacted=\"}";

    private static final String LEGACY_NOTIFICATION = "{\"type\":\"SPLIT_UPDATE\",\"changeNumber\":1684265694505}";

    @Test
    public void nullValuesAreAllowed() {
        SplitsChangeNotification splitsChangeNotification = Json.fromJson(LEGACY_NOTIFICATION, SplitsChangeNotification.class);

        assertEquals(1684265694505L, splitsChangeNotification.getChangeNumber());
        assertNull(splitsChangeNotification.getPreviousChangeNumber());
        assertNull(splitsChangeNotification.getData());
        assertNull(splitsChangeNotification.getCompressionType());
    }

    @Test
    public void valuesAreCorrectlyDeserialized() {
        SplitsChangeNotification c0Notification = Json.fromJson(FULL_NOTIFICATION_C0, SplitsChangeNotification.class);
        SplitsChangeNotification c1Notification = Json.fromJson(FULL_NOTIFICATION_C1, SplitsChangeNotification.class);
        SplitsChangeNotification c2Notification = Json.fromJson(FULL_NOTIFICATION_C2, SplitsChangeNotification.class);

        assertEquals(CompressionType.NONE, c0Notification.getCompressionType());
        assertEquals(CompressionType.GZIP, c1Notification.getCompressionType());
        assertEquals(1684265694505L, c2Notification.getChangeNumber());
        assertEquals(0L, c2Notification.getPreviousChangeNumber().longValue());
        assertEquals("redacted=", c2Notification.getData());
        assertEquals(CompressionType.ZLIB, c2Notification.getCompressionType());
    }
}
