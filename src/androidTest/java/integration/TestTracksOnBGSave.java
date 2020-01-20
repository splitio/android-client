package integration;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.ProcessLifecycleOwner;

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

import fake.HttpClientStub;
import fake.ImpressionsManagerStub;
import fake.MySegmentsCacheStub;
import fake.RefreshableMySegmentsFetcherProviderStub;
import fake.RefreshableSplitFetcherProviderStub;
import fake.SplitCacheStub;
import io.split.android.client.EventPropertiesProcessor;
import io.split.android.client.TrackClient;
import io.split.android.client.TrackClientImpl;
import io.split.android.client.dtos.Event;
import io.split.android.client.lifecycle.LifecycleManager;
import io.split.android.client.storage.legacy.FileStorage;
import io.split.android.client.track.EventsChunk;
import io.split.android.client.track.ITrackStorage;
import io.split.android.client.track.TrackClientConfig;
import io.split.android.client.storage.legacy.TrackStorageManager;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TestTracksOnBGSave {


    @Before
    public void setup() {
    }

    @Test
    public void test() throws URISyntaxException, InterruptedException {

        final String FILE_PREFIX = "SPLITIO.events_chunk_id_";
        File rootFolder = InstrumentationRegistry.getInstrumentation().getContext().getCacheDir();
        File folder = new File(rootFolder, "test_folder");
        if(folder.exists()) {
            for(File file : folder.listFiles()){
                file.delete();
            }
            folder.delete();
        }

        ITrackStorage fileStorage = new FileStorage.TracksFileStorage(rootFolder, "test_folder");
        TrackStorageManager storageManager = new TrackStorageManager(fileStorage);

        LifecycleRegistry lfRegistry = new LifecycleRegistry(ProcessLifecycleOwner.get());
        int chunksCount = 3;
        int maxQueueSize = 100;
        int eventsCount = chunksCount * maxQueueSize;
        TrackClientConfig config = new TrackClientConfig();
        config.setMaxQueueSize(maxQueueSize);
        config.setFlushIntervalMillis(100000);
        config.setWaitBeforeShutdown(100000);
        config.setMaxEventsPerPost(200000);
        TrackClient trackClient = TrackClientImpl.create(
                config, new HttpClientStub(),
                URI.create("https://kubernetesturl.com/split"),
                storageManager);

        LifecycleManager lifecycleManager = new LifecycleManager(new ImpressionsManagerStub(), trackClient,
                new RefreshableSplitFetcherProviderStub(), new RefreshableMySegmentsFetcherProviderStub()
                , new MySegmentsCacheStub());

        lfRegistry.addObserver(lifecycleManager);

        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);


        for (int i = 0; i < eventsCount; ++i) {
            trackClient.track(newEvent("i" + i));
        }

        Thread.sleep(2000);
        List<EventsChunk> eventsBefore = new ArrayList(storageManager.getEventsChunks());

        for (int i = 0; i < maxQueueSize / 2; ++i) {
            trackClient.track(newEvent("i" + i));
        }

        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

        Thread.sleep(2000);

        List<EventsChunk> eventsAfter = new ArrayList(storageManager.getEventsChunks());

        List<String> files = fileStorage.getAllIds(FILE_PREFIX);

        TrackStorageManager storageManagerLoader = new TrackStorageManager(fileStorage);

        List<EventsChunk> impLoaded = new ArrayList(storageManagerLoader.getEventsChunks());
        List<String> afterLoadfiles = fileStorage.getAllIds(FILE_PREFIX);

        Assert.assertEquals(chunksCount, eventsBefore.size());
        Assert.assertEquals(chunksCount + 1, eventsAfter.size());
        Assert.assertEquals(chunksCount + 1, files.size());
        Assert.assertEquals(chunksCount + 1, impLoaded.size());
        Assert.assertEquals(0, afterLoadfiles.size());


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
