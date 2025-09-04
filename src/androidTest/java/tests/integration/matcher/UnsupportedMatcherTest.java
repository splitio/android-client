package tests.integration.matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static helper.IntegrationHelper.getSinceFromUri;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import helper.DatabaseHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitResult;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import tests.integration.shared.TestingHelper;

public class UnsupportedMatcherTest {

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private HttpClientMock mHttpClient;
    private SplitRoomDatabase mDatabase;
    private CountDownLatch mAuthLatch;

    @Before
    public void setUp() throws IOException {
        mAuthLatch = new CountDownLatch(1);
        mDatabase = DatabaseHelper.getTestDatabase(mContext);
        mDatabase.clearAllTables();
        mHttpClient = new HttpClientMock(getDispatcher());
    }

    @Test
    public void featureFlagWithUnsupportedMatcherIsPresentInManager() throws InterruptedException {
        SplitFactory splitFactory = initSplitFactory(new TestableSplitConfigBuilder(), mHttpClient);

        int managerSplitsCount = splitFactory.manager().splits().size();
        int managerSplitNamesCount = splitFactory.manager().splitNames().size();

        assertEquals(1, managerSplitsCount);
        assertEquals(1, managerSplitNamesCount);
    }

    @Test
    public void getTreatmentForUnsupportedMatcherFeatureFlagReturnsControl() throws InterruptedException {
        SplitFactory splitFactory = initSplitFactory(new TestableSplitConfigBuilder(), mHttpClient);

        String treatment = splitFactory.client().getTreatment("feature_flag_for_test");

        assertEquals("control", treatment);
    }

    @Test
    public void getTreatmentWithConfigForUnsupportedMatcherFeatureFlagReturnsControl() throws InterruptedException {
        SplitFactory splitFactory = initSplitFactory(new TestableSplitConfigBuilder(), mHttpClient);

        SplitResult treatment = splitFactory.client().getTreatmentWithConfig("feature_flag_for_test", null);

        assertEquals("control", treatment.treatment());
    }

    @Test
    public void storedImpressionHasUnsupportedLabel() throws InterruptedException {
        SplitFactory splitFactory = initSplitFactory(new TestableSplitConfigBuilder(), mHttpClient);

        splitFactory.client().getTreatmentWithConfig("feature_flag_for_test", null);
        Thread.sleep(200);
        List<ImpressionEntity> storedImpressions = mDatabase.impressionDao().getAll();

        assertEquals(1, storedImpressions.size());
        assertTrue(storedImpressions.get(0).getBody().contains("targeting rule type unsupported by sdk"));
    }

    @Test
    public void impressionInListenerHasUnsupportedLabel() throws InterruptedException {
        AtomicReference<Impression> impression = new AtomicReference<>();
        SplitFactory splitFactory = initSplitFactory(new TestableSplitConfigBuilder().impressionListener(new ImpressionListener() {
            @Override
            public void log(Impression imp) {
                impression.set(imp);
            }

            @Override
            public void close() {

            }
        }), mHttpClient);

        splitFactory.client().getTreatmentWithConfig("feature_flag_for_test", null);

        assertEquals("targeting rule type unsupported by sdk", impression.get().appliedRule());
    }

    private HttpResponseMockDispatcher getDispatcher() {
        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();
        responses.put("splitChanges", (uri, httpMethod, body) -> {
            String since = getSinceFromUri(uri);
            if (since.equals("-1")) {
                return new HttpResponseMock(200, loadSplitChanges());
            } else {
                return new HttpResponseMock(200, IntegrationHelper.emptySplitChanges(1602796638344L, 1602796638344L));
            }
        });

        responses.put(IntegrationHelper.ServicePath.MEMBERSHIPS + "/" + "/test", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptyMySegments()));

        responses.put("v2/auth", (uri, httpMethod, body) -> {
            mAuthLatch.countDown();
            return new HttpResponseMock(200, IntegrationHelper.streamingEnabledToken());
        });

        return IntegrationHelper.buildDispatcher(responses);
    }

    private SplitFactory initSplitFactory(TestableSplitConfigBuilder builder, HttpClientMock httpClient) throws InterruptedException {
        CountDownLatch innerLatch = new CountDownLatch(1);
        SplitFactory factory = IntegrationHelper.buildFactory(
                "sdk_key_1",
                new Key("test"),
                builder
                        .enableDebug()
                        .build(),
                mContext,
                httpClient,
                mDatabase,
                null);

        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(innerLatch));
        boolean await = innerLatch.await(5, TimeUnit.SECONDS);
        if (!await) {
            fail("Client is not ready");
        }

        return factory;
    }

    private String loadSplitChanges() {
        return IntegrationHelper.loadSplitChanges(mContext, "splitchanges_unsupported_matcher.json");
    }
}
