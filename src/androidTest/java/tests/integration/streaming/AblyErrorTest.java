package tests.integration.streaming;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.split.android.client.utils.Logger;

public class AblyErrorTest extends AblyErrorBaseTest {

    @Test
    public void ablyNoRetryableErrorTest() throws IOException, InterruptedException {
        // Retryable if  code >= 40140 &&  code <= 40149;
        initializeFactory();

        for (int i = 0; i < 2; i++) {
            mSseLatch = new CountDownLatch(1);
            pushErrorMessage(40012);
            mSseLatch.await(2, TimeUnit.SECONDS);
        }

        Assert.assertEquals(1, mSseHitCount);
    }

    @Test
    public void ablyRetryableErrorTest() throws IOException, InterruptedException {
        initializeFactory();
        // Retryable if  code >= 40140 &&  code <= 40149;
        for (int i = 0; i < 3; i++) {
            mSseLatch = new CountDownLatch(1);
            pushErrorMessage(40142);
            mSseLatch.await(5, TimeUnit.SECONDS);
        }

        Assert.assertEquals(4, mSseHitCount);
    }
}
