import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fake.EventsManagerMock;
import fake.MetricsMock;
import io.split.android.client.HttpMySegmentsFetcher;
import io.split.android.client.HttpSplitChangeFetcher;
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
import io.split.android.client.impressions.IImpressionsStorage;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.impressions.ImpressionsFileStorage;
import io.split.android.client.impressions.ImpressionsManagerConfig;
import io.split.android.client.impressions.ImpressionsManagerImpl;
import io.split.android.client.impressions.ImpressionsStorageManager;
import io.split.android.client.impressions.ImpressionsStorageManagerConfig;
import io.split.android.client.lifecycle.LifecycleManager;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.storage.FileStorage;
import io.split.android.client.storage.IStorage;
import io.split.android.client.track.ITrackStorage;
import io.split.android.client.track.TrackClientConfig;
import io.split.android.client.track.TrackStorageManager;
import io.split.android.client.track.TracksFileStorage;
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

    final int SERVER_HIT_RATE_SECS = 3;
    final int PAUSE_WAIT_MILLIS = SERVER_HIT_RATE_SECS * 5 * 1000;
    final int HIT_WAIT_SECS = SERVER_HIT_RATE_SECS * 5;
    Context mContext;
    MockWebServer mWebServer;
    ImpressionListener mImpressionsManager;
    TrackClient mTrackClient;
    SplitFetcher mSplitFetcher;
    List<CountDownLatch> mLatchs;

    boolean mMySegmentsHitDone = false;
    boolean mSplitsHitDone = false;
    boolean mImpressionsHitDone = false;
    boolean mTracksHitDone = false;

    boolean mFullHitDone = false;
    int fullHitsLatchIndex = 0;



    @Before
    public void setup() {
        setupServer();
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mMySegmentsHitDone = false;
        mSplitsHitDone = false;
        mImpressionsHitDone = false;
        mTracksHitDone = false;
        mLatchs = new ArrayList<>();

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
                    mMySegmentsHitDone = true;
                    response = new MockResponse().setResponseCode(200).setBody("{\"mySegments\":[]}");
                    log("my segments HIT!!!");
                } else if (request.getPath().contains("/splitChanges")) {
                    mSplitsHitDone = true;
                    response = new MockResponse().setResponseCode(200)
                            .setBody("{\"splits\":[], \"since\":1, \"till\":2}");
                    log("split changes HIT!!!");
                } else if (request.getPath().contains("/events/bulk")) {
                    mTracksHitDone = true;
                    response = new MockResponse().setResponseCode(200);
                    log("events HIT!!!");
                } else {
                    mImpressionsHitDone = true;
                    response = new MockResponse().setResponseCode(200);
                    log("impressions HIT!!!");
                }

                if (mImpressionsHitDone && mTracksHitDone && mMySegmentsHitDone && mSplitsHitDone) {
                    mFullHitDone = true;
                    cleanHitVars();
                    mLatchs.get(fullHitsLatchIndex).countDown();
                    fullHitsLatchIndex++;
                    log("4 HIT!!!");
                }

                return response;
            }
        };
        mWebServer.setDispatcher(dispatcher);
    }

    @Test
    public void pausing1() throws Exception {
        final int TEST_COUNT = 15;

        int latchCount = TEST_COUNT / 2;

        if(!isEven(TEST_COUNT)){
            latchCount++;
        }
        for(int i=0; i<latchCount; i++) {
            mLatchs.add(new CountDownLatch(1));
        }

        LifecycleManager lifecycleManager = createLifecycleManager();

        LifecycleRegistry lfRegistry = new LifecycleRegistry(ProcessLifecycleOwner.get());


        lfRegistry.addObserver(lifecycleManager);

        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        Impression impression = new Impression("key", null, "feature",
                "on", 1111, "default rule",
                999L, null);
        Event event = new Event();
        event.key = "key";
        event.eventTypeId = "typeid";
        event.trafficTypeName = "custom";
        event.timestamp = 999L;


        List<Boolean> hitsResults = new ArrayList<>();

        for(int i=0; i<TEST_COUNT; i++) {
            mImpressionsManager.log(impression);
            mTrackClient.track(event);
            if (isEven(i)) {
                lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
                log("Wait " + i);
                int latchIndex = i / 2;
                mLatchs.get(latchIndex).await(HIT_WAIT_SECS * 10, TimeUnit.SECONDS);
                hitsResults.add(new Boolean(mFullHitDone));
                mFullHitDone = false;
            } else {

                lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
                cleanHitVars();
                log("Wait " + i);
                Thread.sleep(PAUSE_WAIT_MILLIS);
                hitsResults.add(new Boolean(isHitServerDone()));
            }
        }

        for(int i=0; i<TEST_COUNT; i++) {
            if(isEven(i)) {
                Assert.assertTrue(hitsResults.get(i).booleanValue());
            } else {
                Assert.assertFalse(hitsResults.get(i).booleanValue());
            }
        }
    }

    private boolean isEven(int index) {
        return (index % 2) == 0;
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
                new RefreshableMySegmentsFetcherProviderImpl(mySegmentsFetcher, SERVER_HIT_RATE_SECS,
                        key.matchingKey(), new EventsManagerMock());

        SplitParser splitParser = new SplitParser(segmentFetcher);

        // Feature Changes
        IStorage fileStorage = new FileStorage(mContext.getCacheDir(), dataFolderName);
        ISplitCache splitCache = new SplitCache(fileStorage);
        ISplitChangeCache splitChangeCache = new SplitChangeCache(splitCache);

        SplitChangeFetcher splitChangeFetcher = HttpSplitChangeFetcher.create(httpClient, rootTarget, new MetricsMock(), splitChangeCache);
        final RefreshableSplitFetcherProvider splitFetcherProvider = new RefreshableSplitFetcherProviderImpl(
                splitChangeFetcher, splitParser, SERVER_HIT_RATE_SECS, new EventsManagerMock(), splitCache.getChangeNumber());


        // Impressionss
        ImpressionsStorageManagerConfig impressionsStorageManagerConfig = new ImpressionsStorageManagerConfig();
        impressionsStorageManagerConfig.setImpressionsMaxSentAttempts(3);
        impressionsStorageManagerConfig.setImpressionsChunkOudatedTime(99999);
        IImpressionsStorage impressionsStorage = new ImpressionsFileStorage(mContext.getCacheDir(), dataFolderName);
        final ImpressionsStorageManager impressionsStorageManager = new ImpressionsStorageManager(impressionsStorage, impressionsStorageManagerConfig);
        ImpressionsManagerConfig impressionsManagerConfig =
                new ImpressionsManagerConfig(9999, 9999, 9999, SERVER_HIT_RATE_SECS, url);
        final ImpressionsManagerImpl impressionsManager = ImpressionsManagerImpl.instance(httpClient, impressionsManagerConfig, impressionsStorageManager);
        final ImpressionListener impressionListener;

        TrackClientConfig trackConfig = new TrackClientConfig();
        trackConfig.setFlushIntervalMillis(SERVER_HIT_RATE_SECS);
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

    private void cleanHitVars() {
        mImpressionsHitDone = false;
        mSplitsHitDone = false;
        mTracksHitDone = false;
        mMySegmentsHitDone = false;
    }

    private boolean isHitServerDone() {
        return mImpressionsHitDone && mTracksHitDone && mMySegmentsHitDone && mSplitsHitDone;
    }

    private void log(String m) {
        System.out.println("PauseBGProcessesTest: " + m);
    }

}
