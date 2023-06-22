package io.split.android.client.service.sseclient.sseclient.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.split.android.client.common.CompressionType;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.utils.Json;

public class SplitsChangeNotificationTest {

    private static final String FULL_NOTIFICATION = "{\"type\":\"SPLIT_UPDATE\",\"changeNumber\":1684265694505,\"pcn\":0,\"c\":2,\"d\":\"redacted=\"}";

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
        SplitsChangeNotification splitsChangeNotification = Json.fromJson(FULL_NOTIFICATION, SplitsChangeNotification.class);

        assertEquals(1684265694505L, splitsChangeNotification.getChangeNumber());
        assertEquals(0L, splitsChangeNotification.getPreviousChangeNumber().longValue());
        assertEquals("redacted=", splitsChangeNotification.getData());
        assertEquals(CompressionType.ZLIB, splitsChangeNotification.getCompressionType());
    }
}
