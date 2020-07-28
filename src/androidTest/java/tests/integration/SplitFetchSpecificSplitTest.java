package tests.integration;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import helper.FileHelper;
import helper.ImpressionListenerHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.SplitFilter;
import io.split.android.client.SplitManager;
import io.split.android.client.SplitResult;
import io.split.android.client.SyncConfig;
import io.split.android.client.api.Key;
import io.split.android.client.api.SplitView;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.impressions.Impression;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.grammar.Treatments;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@SuppressWarnings({"UnstableApiUsage", "ResultOfMethodCallIgnored"})
public class SplitFetchSpecificSplitTest {

    Context mContext;
    MockWebServer mWebServer;
    int mCurSplitReqId = 1;
    List<String> mTrackEndpointHits = null;
    List<String> mJsonChanges = null;
    CountDownLatch mLatchTrack = null;

    String mReceivedQueryString = null;

    @Before
    public void setup() {
        setupServer();
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mCurSplitReqId = 1;
        mTrackEndpointHits = new ArrayList<>();
        mLatchTrack = null;
        if (mJsonChanges == null) {
            loadSplitChanges();
        }
    }

    private void setupServer() {
        mWebServer = new MockWebServer();

        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().contains("/mySegments")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}");
                } else if (request.getPath().contains("/splitChanges")) {
                    mReceivedQueryString = request.getRequestUrl().query();
                    return new MockResponse().setResponseCode(200)
                            .setBody(IntegrationHelper.emptySplitChanges(-1, 10000));
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };
        mWebServer.setDispatcher(dispatcher);
    }

    @Test
    public void testAll() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch readyFromCacheLatch = new CountDownLatch(1);
        mLatchTrack = new CountDownLatch(10);
        String apiKey = IntegrationHelper.dummyApiKeyAndDb().first;
        String dataFolderName = IntegrationHelper.dummyApiKeyAndDb().second;

        SplitRoomDatabase splitRoomDatabase = SplitRoomDatabase.getDatabase(mContext, dataFolderName);
        splitRoomDatabase.clearAllTables();
        splitRoomDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.DATBASE_MIGRATION_STATUS, GeneralInfoEntity.DATBASE_MIGRATION_STATUS_DONE));
        splitRoomDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 2));
        SplitClient client;
        SplitManager manager;

        final String url = mWebServer.url("/").url().toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(url).eventsEndpoint(url).build();
        Key key = new Key("CUSTOMER_ID");

        SyncConfig syncConfig = SyncConfig.builder().addSplitFilter(SplitFilter.byName(Arrays.asList("split1", "split2","ausgefüllt")))
        .addSplitFilter(SplitFilter.byPrefix(Arrays.asList("pre1", "pre2", "abc\u0223"))).build();

        SplitClientConfig config = SplitClientConfig.builder()
                .serviceEndpoints(endpoints)
                .ready(30000)
                .featuresRefreshRate(30)
                .segmentsRefreshRate(30)
                .impressionsRefreshRate(99999999)
                .eventFlushInterval(9999999)
                .syncConfig(syncConfig)
                .enableDebug()
                .trafficType("account")
                .eventsPerPush(10)
                .eventsQueueSize(100)
                .build();


        SplitFactory splitFactory = SplitFactoryBuilder.build(apiKey, key, config, mContext);

        client = splitFactory.client();
        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);

        client.on(SplitEvent.SDK_READY, readyTask);

        latch.await(10, TimeUnit.SECONDS);

        Assert.assertEquals("names=ausgefüllt,split1,split2&prefixes=abc\u0223,pre1,pre2&since=-1", mReceivedQueryString);

        splitFactory.destroy();
    }

    @After
    public void tearDown() throws IOException {
        mWebServer.shutdown();
    }

    private String splitsPerRequest(int reqId) {
        int req = mJsonChanges.size() - 1;
        if (reqId < req) {
            req = reqId;
        }
        return mJsonChanges.get(req);
    }

    private void loadSplitChanges() {
        FileHelper fileHelper = new FileHelper();
        mJsonChanges = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            String changes = fileHelper.loadFileContent(mContext, "split_changes_" + (i + 1) + ".json");
            mJsonChanges.add(changes);
        }
    }

    private List<Event> buildEventsFromJson(String attributesJson) {

        GsonBuilder gsonBuilder = new GsonBuilder();

        Type mapType = new TypeToken<List<Event>>() {
        }.getType();
        Gson gson = gsonBuilder.create();
        List<Event> events;
        try {
            events = gson.fromJson(attributesJson, mapType);
        } catch (Exception e) {
            events = Collections.emptyList();
        }

        return events;
    }

    private Event findEventWithValue(List<Event> events, double value) {
        for (Event event : events) {
            if (value == event.value) {
                return event;
            }
        }
        return null;
    }

    private void log(String m) {
        System.out.println("FACTORY_TEST: " + m);
    }

}
