package io.split.android.client.telemetry;

import org.junit.Assert;
import org.junit.Test;

public class TelemetryHelperImplTest {

    @Test
    public void test() {
        boolean success = false;

        for (int i = 0; i < 10000; i++) {
            if (new TelemetryHelperImpl().shouldRecordTelemetry()) {
                success = true;
                break;
            }
        }

        Assert.assertTrue(success);
    }
}
