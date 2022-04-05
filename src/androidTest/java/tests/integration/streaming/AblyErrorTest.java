package tests.integration.streaming;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.split.android.client.utils.Logger;

public class AblyErrorTest extends AblyErrorBaseTest {

    @Test
    public void ablyErrorTest() throws IOException, InterruptedException {
        initializeFactory();

        for (int i=0; i<3; i++) {
            mSseLatch = new CountDownLatch(1);
            pushErrorMessage(40012);
            Logger.d("push i: " + i);
            mSseLatch.await(5, TimeUnit.SECONDS);
        }

        Assert.assertEquals(3, mSseHitCount);
    }
}
