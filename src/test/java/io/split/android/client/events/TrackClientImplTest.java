package io.split.android.client.events;

import com.google.common.base.Strings;

import io.split.android.client.TrackClient;
import io.split.android.client.TrackClientImpl;
import io.split.android.client.dtos.Event;
import io.split.android.client.storage.legacy.FileStorage;
import io.split.android.client.track.TrackClientConfig;
import io.split.android.client.storage.legacy.TrackStorageManager;
import io.split.android.fake.ExecutorServiceMock;
import io.split.android.fake.HttpClientMock;
import io.split.android.fake.SplitCacheStub;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TrackClientImplTest {

    @Before
    public void setup() {

    }

    @Test
    public void testEventsFlushedWhenSizeLimitReached() throws URISyntaxException, InterruptedException, IOException {

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorServiceMock senderExecutor = new ExecutorServiceMock(latch);

        TrackClientConfig config = new TrackClientConfig();
        config.setMaxQueueSize(10000);
        config.setFlushIntervalMillis(100000);
        config.setWaitBeforeShutdown(100000);
        config.setMaxEventsPerPost(10000);
        TrackClient eventClient = TrackClientImpl.create(
                config, new HttpClientMock(),
                URI.create("https://kubernetesturl.com/split"),
                new TrackStorageManager(new FileStorage.TracksFileStorage(new File("./build"), "thefoldertest")), new SplitCacheStub(new ArrayList<>()), senderExecutor);

        for (int i = 0; i < 165; ++i) {
            eventClient.track(create32kbEvent());
        }

        int prevSubmitCount = senderExecutor.getSubmitCount();

        eventClient.track(create32kbEvent());

        latch.await(15, TimeUnit.SECONDS);

        Assert.assertEquals(0, prevSubmitCount);
        Assert.assertEquals(1, senderExecutor.getSubmitCount());
    }

    @Test
    public void testEventsFlushedWhenCountLimitReached() throws URISyntaxException, InterruptedException, IOException {

        CountDownLatch latch = new CountDownLatch(5);
        ExecutorServiceMock senderExecutor = new ExecutorServiceMock(latch);

        TrackClientConfig config = new TrackClientConfig();
        config.setMaxQueueSize(10);
        config.setFlushIntervalMillis(999999);
        config.setWaitBeforeShutdown(100000);
        config.setMaxEventsPerPost(2);
        TrackClient eventClient = TrackClientImpl.create(
                config, new HttpClientMock(),
                URI.create("https://kubernetesturl.com/split"),
                new TrackStorageManager(new FileStorage.TracksFileStorage(new File("./build"), "thefoldertest")), new SplitCacheStub(new ArrayList<>()), senderExecutor);

        for (int i = 0; i < 9; ++i) {
            eventClient.track(create32kbEvent());
        }

        Thread.sleep(5000);
        int prevSubmitCount = senderExecutor.getSubmitCount();

        eventClient.track(create32kbEvent()); // 159 32kb events should be about to flush

        latch.await(5, TimeUnit.SECONDS);

        Assert.assertEquals(0, prevSubmitCount);
        Assert.assertEquals(5, senderExecutor.getSubmitCount());
    }


    @Test
    public void testEventsFlushedWhenCountLimitExceded() throws URISyntaxException, InterruptedException, IOException {

        CountDownLatch latch = new CountDownLatch(6);
        ExecutorServiceMock senderExecutor = new ExecutorServiceMock(latch);

        TrackClientConfig config = new TrackClientConfig();
        config.setMaxQueueSize(10);
        config.setFlushIntervalMillis(999999);
        config.setWaitBeforeShutdown(100000);
        config.setMaxEventsPerPost(2);
        TrackClient eventClient = TrackClientImpl.create(
                config, new HttpClientMock(),
                URI.create("https://kubernetesturl.com/split"),
                new TrackStorageManager(new FileStorage.TracksFileStorage(new File("./build"), "thefoldertest")), new SplitCacheStub(new ArrayList<>()), senderExecutor);

        for (int i = 0; i < 10; ++i) {
            eventClient.track(create32kbEvent());
        }

        latch.await(5, TimeUnit.SECONDS);
        latch = new CountDownLatch(1);
        senderExecutor.setLatch(latch);

        int prevSubmitCount = senderExecutor.getSubmitCount();

        eventClient.track(create32kbEvent()); // 159 32kb events should be about to flush

        latch.await(5, TimeUnit.SECONDS);

        Assert.assertEquals(5, prevSubmitCount);
        Assert.assertEquals(5, senderExecutor.getSubmitCount());
    }

    private Event create32kbEvent() {
        Event event = new Event();
        event.trafficTypeName = "custom";
        event.eventTypeId = "type";
        event.timestamp = 111111;
        event.key = "validkey";
        Map<String, Object> props = new HashMap<>();
        props.put("key", Strings.repeat("a", 1024 * 30));
        event.properties = props;
        return event;
    }

}

