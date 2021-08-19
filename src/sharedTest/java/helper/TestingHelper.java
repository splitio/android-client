package helper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventTaskMethodNotImplementedException;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.utils.Logger;

import static java.lang.Thread.sleep;

public class TestingHelper {

    public static final String COUNTERS_REFRESH_RATE_SECS_NAME = "COUNTERS_REFRESH_RATE_SECS";
    public static final String MSG_DATA_FIELD = "[NOTIFICATION_DATA]";

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
        public boolean onExecutedCalled = false;

        TestEventTask() {
            this(null);
        }

        TestEventTask(CountDownLatch latch) {
            mLatch = latch;
        }

        public void onPostExecution(SplitClient client) {
            onExecutedCalled = true;
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

    static public void pushKeepAlive(BlockingQueue<String> streamingData) {
        try {
            streamingData.put(":keepalive" + "\n");
            Logger.d("Pushed initial ID");
        } catch (InterruptedException e) {
            Logger.d("Error Pushed initial ID: " + e.getLocalizedMessage());
        }
    }
}
