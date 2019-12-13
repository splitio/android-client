package integration;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.base.Strings;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import fake.ImpressionsManagerStub;
import fake.MySegmentsCacheStub;
import fake.RefreshableMySegmentsFetcherProviderStub;
import fake.RefreshableSplitFetcherProviderStub;
import fake.SplitCacheStub;
import fake.TrackClientStub;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.SplitResult;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.Condition;
import io.split.android.client.dtos.DependencyMatcherData;
import io.split.android.client.dtos.Matcher;
import io.split.android.client.dtos.MatcherType;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.lifecycle.LifecycleManager;
import io.split.android.client.utils.Json;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MultiFactoryTreatmentOnBGSaveTest {

    Context mContext;
    MockWebServer mWebServer;
    Map<Long, String> mJsonChanges = null;
    final static int CHANGE_INTERVAL  = 1000;
    CountDownLatch mReadyLatch;
    CountDownLatch mLatch;

    @Before
    public void setup() {

        mLatch = new CountDownLatch(2);
        mReadyLatch = new CountDownLatch(2);
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        if(mJsonChanges == null) {
            loadSplitChanges();
        }
        setupServer();
    }

    private void setupServer() {
        mWebServer = new MockWebServer();

        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch (RecordedRequest request) throws InterruptedException {
                if (request.getPath().contains("/mySegments")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}");
                } else if (request.getPath().contains("/splitChanges")) {
                    long r = Long.parseLong(request.getRequestUrl().queryParameter("since"));
                    String respData = getChangesResponse(r);
                    return new MockResponse().setResponseCode(200)
                            .setBody(respData);
                } else {
                    return new MockResponse().setResponseCode(200);
                }
            }
        };
        mWebServer.setDispatcher(dispatcher);
    }

    @Test
    public void test() throws URISyntaxException, InterruptedException, IOException, TimeoutException {
        final int EVALUATION_COUNT = 100;
        SplitClient c1 = newClient();
        SplitClient c2 = newClient();

        List<SplitResult> r1 = Collections.synchronizedList(new ArrayList<>());
        List<SplitResult> r2 = Collections.synchronizedList(new ArrayList<>());

        LifecycleRegistry lfRegistry = new LifecycleRegistry(ProcessLifecycleOwner.get());

        LifecycleManager lifecycleManager = new LifecycleManager(new ImpressionsManagerStub(), new TrackClientStub(),
                new RefreshableSplitFetcherProviderStub(), new RefreshableMySegmentsFetcherProviderStub(),
                new SplitCacheStub(), new MySegmentsCacheStub());

        lfRegistry.addObserver(lifecycleManager);

        mReadyLatch.await(40, TimeUnit.SECONDS);

        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < EVALUATION_COUNT; i++) {
                    r1.add(c1.getTreatmentWithConfig("dep_split", null));
                    r1.add(c1.getTreatmentWithConfig("split_5", null));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                mLatch.countDown();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < EVALUATION_COUNT; i++) {
                    r2.add(c2.getTreatmentWithConfig("dep_split", null));
                    r2.add(c2.getTreatmentWithConfig("split_3", null));
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                mLatch.countDown();
            }
        }).start();

        Thread lifeCycleThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException e) {
                    }

                    lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException e) {
                    }
                    lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
                }
            }
        });
        lifeCycleThread.start();

        mLatch.await(60, TimeUnit.SECONDS);

        Assert.assertEquals(EVALUATION_COUNT * 2, r1.size());
        Assert.assertEquals(EVALUATION_COUNT * 2, r2.size());

        Assert.assertEquals("on", r1.get(0).treatment());
        Assert.assertEquals("on", r1.get(1).treatment());

        Assert.assertEquals("on", r2.get(0).treatment());
        Assert.assertEquals("on", r2.get(1).treatment());
    }

    private void loadSplitChanges() {
        FileHelper fileHelper = new FileHelper();
        mJsonChanges = new ConcurrentHashMap<>();
        String jsonSplit = fileHelper.loadFileContent(mContext, "split_dependency.json");

        Split depSplit = Json.fromJson(jsonSplit, Split.class);

        long prevChangeNumber = -1;
        final int CHANGE_COUNT = 10;
        final int DEPENDENCY_COUNT = 10;
        for(int j = 0; j < CHANGE_COUNT; j++) {
            SplitChange change  = new SplitChange();
            change.splits = new ArrayList<>();
            change.splits.add(depSplit);
            change.since = prevChangeNumber;
            if(prevChangeNumber == -1) {
                prevChangeNumber++;
            }
            prevChangeNumber += CHANGE_INTERVAL;
            change.till = prevChangeNumber;
            for (int i = 0; i < DEPENDENCY_COUNT; i++) {
                Split split = Json.fromJson(jsonSplit, Split.class);
                split.name = "split_" + i;
                split.changeNumber = prevChangeNumber - CHANGE_INTERVAL / 2;
                Map<String, String> cfg = new HashMap<>();
                cfg.put("on", "{\"c\":" + j + "}");
                split.configurations = cfg;
                Condition condition = split.conditions.get(0);
                if(i != DEPENDENCY_COUNT -1) {
                    DependencyMatcherData matcherData = condition.matcherGroup.matchers.get(0).dependencyMatcherData;
                    matcherData.split = "split_" + (i + 1);
                } else {
                    Matcher matcher = condition.matcherGroup.matchers.get(0);
                    matcher.dependencyMatcherData = null;
                    matcher.matcherType = MatcherType.ALL_KEYS;
                }
                change.splits.add(split);
            }
            mJsonChanges.put(change.since, Json.toJson(change));
        }
    }

    private String getChangesResponse(long since) {
        String change =  mJsonChanges.get(since);
        if(Strings.isNullOrEmpty(change)) {
            return IntegrationHelper.emptySplitChanges(since, since + CHANGE_INTERVAL);
        }
        return change;
    }

    private SplitClient newClient() throws InterruptedException, URISyntaxException, TimeoutException, IOException {
        String apiKey = "99049fd8653247c5ea42bc3c1ae2c6a42bc3";
        final String url = mWebServer.url("/").url().toString();

        Key key = new Key("CUSTOMER_ID", null);
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .endpoint(url, url)
                .ready(30000)
                .featuresRefreshRate(3)
                .segmentsRefreshRate(5)
                .eventFlushInterval(IntegrationHelper.NEVER_REFRESH_RATE)
                .impressionsRefreshRate(IntegrationHelper.NEVER_REFRESH_RATE)
                .impressionsQueueSize(IntegrationHelper.NEVER_REFRESH_RATE)
                .impressionsChunkSize(IntegrationHelper.NEVER_REFRESH_RATE)
                .enableDebug()
                .trafficType("client")
                .build();


        SplitFactory splitFactory = SplitFactoryBuilder.build(apiKey, key, config, mContext);

        SplitClient client = splitFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(mReadyLatch);

        client.on(SplitEvent.SDK_READY, readyTask);

        return client;
    }
}
