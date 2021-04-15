package helper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventTaskMethodNotImplementedException;

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

    static public class TestEventTask extends SplitEventTask {

        private CountDownLatch mLatch;

        TestEventTask() {
            this(null);
        }

        TestEventTask(CountDownLatch latch) {
            mLatch = latch;
        }

        public void onPostExecution(SplitClient client) {
            if(mLatch != null) {
                mLatch.countDown();
            }
        }

        public void onPostExecutionView(SplitClient client) {
        }
    }

    static public TestEventTask testTask(CountDownLatch latch)  {
        return new TestEventTask(latch);
    }
}
