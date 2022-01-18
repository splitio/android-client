package io.split.android.client.telemetry;

import org.junit.Assert;
import org.junit.Test;

public class TelemetryHelperImplTest {

    @Test
    public void test() {
        int successCount = 0;

        for (int i = 0; i < 10000; i++) {
            if (new TelemetryHelperImpl().shouldRecordTelemetry()) {
                successCount++;
            }
        }

        Assert.assertTrue(successCount >= 5 && successCount <= 30);
    }
}
