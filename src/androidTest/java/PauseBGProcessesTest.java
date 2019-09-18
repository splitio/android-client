import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fake.EventsManagerMock;
import fake.MetricsMock;
import helper.SplitEventTaskHelper;
import io.split.android.client.HttpMySegmentsFetcher;
import io.split.android.client.HttpSplitChangeFetcher;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.TrackClient;
import io.split.android.client.TrackClientImpl;
import io.split.android.client.api.Key;
import io.split.android.client.cache.IMySegmentsCache;
import io.split.android.client.cache.ISplitCache;
import io.split.android.client.cache.ISplitChangeCache;
import io.split.android.client.cache.MySegmentsCache;
import io.split.android.client.cache.SplitCache;
import io.split.android.client.cache.SplitChangeCache;
import io.split.android.client.dtos.Event;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.impressions.IImpressionsStorage;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.impressions.ImpressionsFileStorage;
import io.split.android.client.impressions.ImpressionsManager;
import io.split.android.client.impressions.ImpressionsManagerConfig;
import io.split.android.client.impressions.ImpressionsManagerImpl;
import io.split.android.client.impressions.ImpressionsStorageManager;
import io.split.android.client.impressions.ImpressionsStorageManagerConfig;
import io.split.android.client.lifecycle.LifecycleManager;
import io.split.android.client.metrics.CachedMetrics;
import io.split.android.client.metrics.FireAndForgetMetrics;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.storage.FileStorage;
import io.split.android.client.storage.IStorage;
import io.split.android.client.track.ITrackStorage;
import io.split.android.client.track.TrackClientConfig;
import io.split.android.client.track.TrackStorageManager;
import io.split.android.client.track.TracksFileStorage;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.Utils;
import io.split.android.engine.experiments.RefreshableSplitFetcherProvider;
import io.split.android.engine.experiments.RefreshableSplitFetcherProviderImpl;
import io.split.android.engine.experiments.SplitChangeFetcher;
import io.split.android.engine.experiments.SplitFetcher;
import io.split.android.engine.experiments.SplitParser;
import io.split.android.engine.segments.MySegmentsFetcher;
import io.split.android.engine.segments.RefreshableMySegmentsFetcherProviderImpl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class PauseBGProcessesTest {

    final int SERVER_HIT_RATE = 3000;
    final int PAUSE_WAIT_SECS = SERVER_HIT_RATE / 1000 * 3;
    final int HIT_WAIT_SECS = SERVER_HIT_RATE / 1000 * 3;
    Context mContext;
    MockWebServer mWebServer;
    volatile boolean  mIsServerHitDone = false;
    ImpressionListener mImpressionsManager;
    TrackClient mTrackClient;
    SplitFetcher mSplitFetcher;
    CountDownLatch mLatch;


    @Before
    public void setup() {
        setupServer();
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mIsServerHitDone = false;
    }

    @After
    public void tearDown() throws IOException {
        mWebServer.shutdown();
    }

    private void setupServer() {
        mWebServer = new MockWebServer();

        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {

                MockResponse response;
                if (request.getPath().contains("/mySegments")) {
                    response = new MockResponse().setResponseCode(200).setBody("{\"mySegments\":[]}");
                } else if (request.getPath().contains("/splitChanges")) {
                    response = new MockResponse().setResponseCode(200)
                            .setBody("{\"splits\":[], \"since\":1, \"till\":2}");
                } else {
                    response = new MockResponse().setResponseCode(200);
                }
                mIsServerHitDone = true;
                log("HIT!!!");
                mLatch.countDown();
                return response;
            }
        };
        mWebServer.setDispatcher(dispatcher);
    }

    @Test
    public void pausing() throws Exception {

        mLatch = new CountDownLatch(3);

        LifecycleManager lifecycleManager = createLifecycleManager();

        LifecycleRegistry lfRegistry = new LifecycleRegistry(ProcessLifecycleOwner.get());


        lfRegistry.addObserver(lifecycleManager);

        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

        Impression impression = new Impression("key", null, "feature",
                "on", 1111, "default rule",
                999L, null);
        Event event = new Event();
        event.key = "key";
        event.eventTypeId = "typeid";
        event.trafficTypeName = "custom";
        event.timestamp = 999L;


        mIsServerHitDone = false;
        mImpressionsManager.log(impression);
        mTrackClient.track(event);

        mLatch.await(HIT_WAIT_SECS, TimeUnit.SECONDS);
        boolean hit1 = mIsServerHitDone;

        mImpressionsManager.log(impression);
        mTrackClient.track(event);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

        mIsServerHitDone = false;
        mLatch.await(PAUSE_WAIT_SECS, TimeUnit.SECONDS);
        boolean hit2 = mIsServerHitDone;

        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        mIsServerHitDone = false;
        mImpressionsManager.log(impression);
        mTrackClient.track(event);
        mLatch.await(HIT_WAIT_SECS, TimeUnit.SECONDS);
        boolean hit3 = mIsServerHitDone;

        mImpressionsManager.log(impression);
        mTrackClient.track(event);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        mIsServerHitDone = false;
        mLatch.await(PAUSE_WAIT_SECS, TimeUnit.SECONDS);
        boolean hit4 = mIsServerHitDone;

        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        mIsServerHitDone = false;
        mImpressionsManager.log(impression);
        mTrackClient.track(event);
        Thread.sleep(HIT_WAIT_SECS);
        boolean hit5 = mIsServerHitDone;

        Assert.assertTrue(hit1);
        Assert.assertFalse(hit2);
        Assert.assertTrue(hit3);
        Assert.assertFalse(hit4);
        Assert.assertTrue(hit5);

    }


    private LifecycleManager createLifecycleManager() throws URISyntaxException {

        String apiKey = "99049fd8653247c5ea42bc3c1ae2c6a42bc3";

        final String url = mWebServer.url("/").url().toString();

        Key key = new Key("CUSTOMER_ID", null);

        SplitHttpHeadersBuilder headersBuilder = new SplitHttpHeadersBuilder();
        headersBuilder.setClientVersion("x.x.x");
        headersBuilder.setApiToken(apiKey);

        final HttpClient httpClient = new HttpClientImpl();
        httpClient.addHeaders(headersBuilder.build());

        URI rootTarget = URI.create(url);
        URI eventsRootTarget = URI.create(url);

        String dataFolderName = Utils.convertApiKeyToFolder(apiKey);
        if (dataFolderName == null) {
            dataFolderName = "test_datafolder";
        }

        // Segments
        IStorage mySegmentsStorage = new FileStorage(mContext.getCacheDir(), dataFolderName);
        IMySegmentsCache mySegmentsCache = new MySegmentsCache(mySegmentsStorage);
        MySegmentsFetcher mySegmentsFetcher = HttpMySegmentsFetcher.create(httpClient, rootTarget,
                mySegmentsCache);
        final RefreshableMySegmentsFetcherProviderImpl segmentFetcher =
                new RefreshableMySegmentsFetcherProviderImpl(mySegmentsFetcher, SERVER_HIT_RATE,
                        key.matchingKey(), new EventsManagerMock());

        SplitParser splitParser = new SplitParser(segmentFetcher);

        // Feature Changes
        IStorage fileStorage = new FileStorage(mContext.getCacheDir(), dataFolderName);
        ISplitCache splitCache = new SplitCache(fileStorage);
        ISplitChangeCache splitChangeCache = new SplitChangeCache(splitCache);

        SplitChangeFetcher splitChangeFetcher = HttpSplitChangeFetcher.create(httpClient, rootTarget, new MetricsMock(), splitChangeCache);
        final RefreshableSplitFetcherProvider splitFetcherProvider = new RefreshableSplitFetcherProviderImpl(
                splitChangeFetcher, splitParser, SERVER_HIT_RATE, new EventsManagerMock(), splitCache.getChangeNumber());


        // Impressionss
        ImpressionsStorageManagerConfig impressionsStorageManagerConfig = new ImpressionsStorageManagerConfig();
        impressionsStorageManagerConfig.setImpressionsMaxSentAttempts(3);
        impressionsStorageManagerConfig.setImpressionsChunkOudatedTime(99999);
        IImpressionsStorage impressionsStorage = new ImpressionsFileStorage(mContext.getCacheDir(), dataFolderName);
        final ImpressionsStorageManager impressionsStorageManager = new ImpressionsStorageManager(impressionsStorage, impressionsStorageManagerConfig);
        ImpressionsManagerConfig impressionsManagerConfig =
                new ImpressionsManagerConfig(9999, 9999, 9999, SERVER_HIT_RATE, url);
        final ImpressionsManagerImpl impressionsManager = ImpressionsManagerImpl.instance(httpClient, impressionsManagerConfig, impressionsStorageManager);
        final ImpressionListener impressionListener;

        TrackClientConfig trackConfig = new TrackClientConfig();
        trackConfig.setFlushIntervalMillis(SERVER_HIT_RATE);
        trackConfig.setMaxEventsPerPost(9);
        trackConfig.setMaxQueueSize(10);
        trackConfig.setWaitBeforeShutdown(1);
        trackConfig.setMaxSentAttempts(3);
        trackConfig.setMaxQueueSizeInBytes(5000);
        ITrackStorage eventsStorage = new TracksFileStorage(mContext.getCacheDir(), dataFolderName);
        TrackStorageManager trackStorageManager = new TrackStorageManager(eventsStorage);
        TrackClient trackClient = TrackClientImpl.create(trackConfig, httpClient, eventsRootTarget, trackStorageManager, splitCache);

        mImpressionsManager = impressionsManager;
        mTrackClient = trackClient;
        mSplitFetcher = splitFetcherProvider.getFetcher();

        return new LifecycleManager(impressionsManager, trackClient, splitFetcherProvider, segmentFetcher, splitCache, mySegmentsCache);


    }

    private void log(String m) {
        System.out.println("FACTORY_TEST: " + m);
    }

}
