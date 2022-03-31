package tests.integration.shared;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import helper.DatabaseHelper;
import helper.IntegrationHelper;
import helper.TestingHelper;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.SplitRoomDatabase;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class InterdependentSplitsTest {

    private Context mContext;
    private MockWebServer mWebServer;
    private SplitRoomDatabase mRoomDb;
    private SplitFactory mSplitFactory;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);

        mWebServer = new MockWebServer();

        String serverUrl = mWebServer.url("/").toString();
        final Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().contains("/mySegments/key1")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setBody("{\"mySegments\":[{ \"id\":\"id0\", \"name\":\"android_test\"}]}");
                } else if (request.getPath().contains("/mySegments/key2")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setBody(IntegrationHelper.emptyMySegments());
                } else if (request.getPath().contains("/splitChanges")) {
                    String splitChange = "{\"splits\":[{\"trafficTypeName\":\"account\",\"name\":\"android_test_2\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-1955610140,\"seed\":-633015570,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1648733409158,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"account\",\"attribute\":null},\"matcherType\":\"IN_SPLIT_TREATMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":{\"split\":\"android_test_3\",\"treatments\":[\"on\"]},\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":100},{\"treatment\":\"off\",\"size\":0}],\"label\":\"in split android_test_3 treatment [on]\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"account\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100}],\"label\":\"default rule\"}]},{\"trafficTypeName\":\"account\",\"name\":\"android_test_3\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-397942789,\"seed\":1852089605,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1648733496087,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"account\",\"attribute\":null},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"android_test\"},\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":100},{\"treatment\":\"off\",\"size\":0}],\"label\":\"in segment android_test\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"account\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100}],\"label\":\"default rule\"}]}],\"since\":-1,\"till\":1648733409158}";

                    return new MockResponse().setResponseCode(200)
                            .setBody(splitChange);
                } else if (request.getPath().contains("/events/bulk")) {
                    return new MockResponse().setResponseCode(200);
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };
        mWebServer.setDispatcher(dispatcher);
        mSplitFactory = IntegrationHelper.buildFactory(IntegrationHelper.dummyApiKey(),
                new Key("key1"),
                SplitClientConfig.builder()
                        .serviceEndpoints(ServiceEndpoints.builder()
                                .apiEndpoint(serverUrl).eventsEndpoint(serverUrl).build())
                        .ready(30000)
                        .enableDebug()
                        .featuresRefreshRate(99999)
                        .segmentsRefreshRate(99999)
                        .impressionsRefreshRate(99999)
                        .trafficType("account")
                        .streamingEnabled(false)
                        .build(),
                mContext,
                null,
                mRoomDb);

        mRoomDb.clearAllTables();
    }

    @After
    public void tearDown() {
        mSplitFactory.destroy();
    }

    @Test
    public void testInterdependentSplits() throws InterruptedException {

        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch readyLatch2 = new CountDownLatch(1);

        SplitClient client = mSplitFactory.client(new Key("key1"));
        SplitClient client2 = mSplitFactory.client(new Key("key2"));
        TestingHelper.TestEventTask testEventTask = TestingHelper.testTask(readyLatch);
        TestingHelper.TestEventTask testEventTask2 = TestingHelper.testTask(readyLatch2);
        client.on(SplitEvent.SDK_READY, testEventTask);
        client2.on(SplitEvent.SDK_READY, testEventTask2);

        readyLatch.await(20, TimeUnit.SECONDS);
        readyLatch2.await(20, TimeUnit.SECONDS);

        assertEquals("on", client.getTreatment("android_test_2"));
        assertEquals("on", client.getTreatment("android_test_3"));

        assertEquals("off", client2.getTreatment("android_test_2"));
        assertEquals("off", client2.getTreatment("android_test_3"));
    }
}
