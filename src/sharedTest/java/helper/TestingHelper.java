package helper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.split.android.client.SplitClient;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.utils.logger.Logger;

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

        public CountDownLatch mLatch;
        public boolean onExecutedCalled = false;
        public String mTitle = "";

        TestEventTask() {
            this(null, "");
        }

        public TestEventTask(CountDownLatch latch) {
            this(latch, "");
        }

        public TestEventTask(CountDownLatch latch, String title) {
            mLatch = latch;
            mTitle = title;
        }

        public void onPostExecution(SplitClient client) {
            System.out.println("Executing onPostExecution: " + mTitle);
            onExecutedCalled = true;
            if(mLatch != null) {
                System.out.println("Latch fired TestEventTask");
                mLatch.countDown();
            }
        }

        public void onPostExecutionView(SplitClient client) {
        }

        public void setLatch(CountDownLatch latch) {
            mLatch = latch;
        }
    }

    static public TestEventTask testTask(CountDownLatch latch)  {
        return new TestEventTask(latch);
    }

    static public TestEventTask testTask(CountDownLatch latch, String title)  {
        return new TestEventTask(latch, title);
    }

    static public void pushKeepAlive(BlockingQueue<String> streamingData) {
        try {
            streamingData.put(":keepalive" + "\n");
            Logger.d("Pushed initial ID");
        } catch (InterruptedException e) {
            Logger.d("Error Pushed initial ID: " + e.getLocalizedMessage());
        }
    }

    public static List<KeyImpression> createImpressions(int from, int to, int status) {
        List<KeyImpression> impressions = new ArrayList<>();
        for(int i = from; i <= to; i++) {
            impressions.add(newImpression("feature_" + i, "key_" + i));
        }
        return impressions;
    }

    public static List<Event> createEvents(int from, int to, int status) {
        List<Event> events = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            Event event = new Event();
            event.eventTypeId = "event_" + i;
            event.trafficTypeName = "custom";
            event.key = "key1";
            events.add(event);
        }
        return events;
    }

    public static KeyImpression newImpression(String feature, String key) {
        KeyImpression impression = new KeyImpression();
        impression.changeNumber = 100L;
        impression.feature = feature;
        impression.keyName = key;
        impression.treatment = "on";
        impression.time = 100;
        return impression;
    }
}
