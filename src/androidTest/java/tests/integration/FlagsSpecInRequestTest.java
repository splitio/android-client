package tests.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static helper.IntegrationHelper.ResponseClosure.getSinceFromUri;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.android_client.test.BuildConfig;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import tests.integration.shared.TestingHelper;

public class FlagsSpecInRequestTest {

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private AtomicInteger mImpressionsListenerCount;
    private HttpClientMock mHttpClient;
    private SplitRoomDatabase mDatabase;
    private AtomicReference<String> mQueryString;

    @Before
    public void setUp() throws IOException {
        mImpressionsListenerCount = new AtomicInteger(0);
        mDatabase = DatabaseHelper.getTestDatabase(mContext);
        mDatabase.clearAllTables();
        mQueryString = new AtomicReference<>();
        mHttpClient = new HttpClientMock(getDispatcher());
    }

    @Test
    public void queryStringContainsFlagsSpec() throws InterruptedException {
        initSplitFactory(new TestableSplitConfigBuilder(), mHttpClient);

        assertEquals("s=1.1&since=-1", mQueryString.get());
    }

    private HttpResponseMockDispatcher getDispatcher() {
        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();
        responses.put("splitChanges", (uri, httpMethod, body) -> {
            String queryString = uri.getQuery();
            mQueryString.set(queryString);
            String since = getSinceFromUri(uri);
            if (since.equals("-1")) {
                return new HttpResponseMock(200, loadSplitChanges());
            } else {
                return new HttpResponseMock(200, IntegrationHelper.emptySplitChanges(1602796638344L, 1602796638344L));
            }
        });

        responses.put("mySegments/CUSTOMER_ID", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptyMySegments()));

        return IntegrationHelper.buildDispatcher(responses);
    }

    private void initSplitFactory(TestableSplitConfigBuilder builder, HttpClientMock httpClient) throws InterruptedException {
        CountDownLatch innerLatch = new CountDownLatch(1);
        SplitFactory factory = IntegrationHelper.buildFactory(
                "sdk_key_1",
                IntegrationHelper.dummyUserKey(),
                builder.build(),
                mContext,
                httpClient,
                mDatabase);

        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(innerLatch));
        boolean await = innerLatch.await(5, TimeUnit.SECONDS);
        if (!await) {
            fail("Client is not ready");
        }
    }

    private String loadSplitChanges() {
        FileHelper fileHelper = new FileHelper();
        String change = fileHelper.loadFileContent(mContext, "split_changes_1.json");
        SplitChange parsedChange = Json.fromJson(change, SplitChange.class);
        parsedChange.since = parsedChange.till;
        return Json.toJson(parsedChange);
    }
}
