package helper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

public class TestingHelper {

    static public void delay(long millis) {

        CountDownLatch waitLatch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sleep(millis);
                    waitLatch.countDown();
                } catch (InterruptedException e) {
                }
            }
        }).start();
        try {
            waitLatch.await(millis + 2000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
    }
}
