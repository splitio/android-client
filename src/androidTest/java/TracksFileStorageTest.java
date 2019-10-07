import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import fake.HttpClientStub;
import fake.ImpressionsManagerStub;
import fake.MySegmentsCacheStub;
import fake.RefreshableMySegmentsFetcherProviderStub;
import fake.RefreshableSplitFetcherProviderStub;
import fake.SplitCacheStub;
import io.split.android.client.TrackClient;
import io.split.android.client.TrackClientImpl;
import io.split.android.client.dtos.Event;
import io.split.android.client.lifecycle.LifecycleManager;
import io.split.android.client.track.EventsChunk;
import io.split.android.client.track.ITrackStorage;
import io.split.android.client.track.TrackClientConfig;
import io.split.android.client.track.TrackStorageManager;
import io.split.android.client.track.TracksFileStorage;

public class TracksFileStorageTest {


    @Before
    public void setup() {
    }

    @Test
    public void test() throws URISyntaxException, InterruptedException {

        CountDownLatch endLatch = new CountDownLatch(1);
        File rootFolder = InstrumentationRegistry.getInstrumentation().getContext().getCacheDir();
        File folder = new File(rootFolder, "test_folder");
        if(folder.exists()) {
            for(File file : folder.listFiles()){
                file.delete();
            }
            folder.delete();
        }

        ITrackStorage fileStorage = new TracksFileStorage(rootFolder, "track_folder_test");
        TrackStorageManager storageManager = new TrackStorageManager(fileStorage);

        AtomicBoolean saved = new AtomicBoolean(false);
        AtomicBoolean generate = new AtomicBoolean(true);
        int chunksCount = 3;
        int maxQueueSize = 100;
        int eventsCount = chunksCount * maxQueueSize;
        TrackClientConfig config = new TrackClientConfig();
        config.setMaxQueueSize(maxQueueSize);
        config.setFlushIntervalMillis(100000);
        config.setWaitBeforeShutdown(100000);
        config.setMaxEventsPerPost(200000);

        final TrackClient trackClient = TrackClientImpl.create(
                config, new HttpClientStub(),
                URI.create("https://kubernetesturl.com/split"),
                storageManager,
                new SplitCacheStub(new ArrayList<>()));

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (generate.get()) {
                    trackClient.track(newEvent("test"));
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                }

            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 10; ++i) {
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                        }
                        storageManager.saveToDisk();
                    }
                    saved.set(true);
                    generate.set(false);
                    endLatch.countDown();
                } catch (Exception e){
                    throw e;
                }
            }
        }).start();

        endLatch.await(60, TimeUnit.SECONDS);
        generate.set(false);
        Assert.assertTrue(saved.get());

    }

    private Event newEvent(String eventType) {
        Event event = new Event();
        event.trafficTypeName = "custom";
        event.eventTypeId = "type" + eventType;
        event.timestamp = 111111;
        event.key = "validkey";
        Map<String, Object> props = new HashMap<>();
        props.put("key1", "value");
        props.put("key2", true);
        props.put("key3", 1.0);
        event.properties = props;
        return event;
    }
}
