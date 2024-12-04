package tests.integration.matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static helper.IntegrationHelper.getSinceFromUri;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import helper.DatabaseHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import tests.integration.shared.TestingHelper;

public class SemverMatcherTest {

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private HttpClientMock mHttpClient;
    private SplitRoomDatabase mDatabase;
    private CountDownLatch mAuthLatch;
    private SplitClient mSplitClient;
    private List<Impression> mImpressionsOnListener;

    @Before
    public void setUp() throws IOException, InterruptedException {
        mAuthLatch = new CountDownLatch(1);
        mDatabase = DatabaseHelper.getTestDatabase(mContext);
        mDatabase.clearAllTables();
        mHttpClient = new HttpClientMock(getDispatcher());
        mImpressionsOnListener = new ArrayList<>();
        mSplitClient = initSplitFactory(new TestableSplitConfigBuilder()
                .impressionsMode(ImpressionsMode.DEBUG)
                .impressionListener(new ImpressionListener() {
                    @Override
                    public void log(Impression impression) {
                        mImpressionsOnListener.add(impression);
                    }

                    @Override
                    public void close() {

                    }
                }), mHttpClient).client();
    }

    @After
    public void tearDown() {
        mHttpClient.close();
        mSplitClient.destroy();
    }

    @Test
    public void equalToSemverMatcher() throws InterruptedException {
        assertEquals("on", mSplitClient.getTreatment("semver_equalto", getVersionAttributesMap("1.22.9")));
        assertEquals("off", mSplitClient.getTreatment("semver_equalto", getVersionAttributesMap("1.22.9+build")));
        assertEquals("off", mSplitClient.getTreatment("semver_equalto", getVersionAttributesMap("1.22.9-rc.0")));
        assertEquals("off", mSplitClient.getTreatment("semver_equalto", getVersionAttributesMap(null)));
        assertEquals("off", mSplitClient.getTreatment("semver_equalto"));

        assertImpressions(1, 4, 5, "equal to semver");
    }

    @Test
    public void inListSemverMatcher() throws InterruptedException {
        assertEquals("on", mSplitClient.getTreatment("semver_inlist", getVersionAttributesMap("2.1.0")));
        assertEquals("on", mSplitClient.getTreatment("semver_inlist", getVersionAttributesMap("1.22.9")));
        assertEquals("off", mSplitClient.getTreatment("semver_inlist", getVersionAttributesMap("1.22.9+build")));
        assertEquals("off", mSplitClient.getTreatment("semver_inlist", getVersionAttributesMap("1.22.9-rc.0")));
        assertEquals("off", mSplitClient.getTreatment("semver_inlist", getVersionAttributesMap(null)));

        assertImpressions(2, 3, 5, "in list semver");
    }

    @Test
    public void greaterThanOrEqualToSemverMatcher() throws InterruptedException {
        assertEquals("on", mSplitClient.getTreatment("semver_greater_or_equalto", getVersionAttributesMap("1.23.9")));
        assertEquals("on", mSplitClient.getTreatment("semver_greater_or_equalto", getVersionAttributesMap("1.22.9")));
        assertEquals("on", mSplitClient.getTreatment("semver_greater_or_equalto", getVersionAttributesMap("1.22.9+build")));
        assertEquals("off", mSplitClient.getTreatment("semver_greater_or_equalto", getVersionAttributesMap("1.22.9-rc.0")));
        assertEquals("off", mSplitClient.getTreatment("semver_greater_or_equalto", getVersionAttributesMap("1.21.9")));
        assertEquals("off", mSplitClient.getTreatment("semver_greater_or_equalto", getVersionAttributesMap("invalid")));

        assertImpressions(3, 3, 6, "greater than or equal to semver");
    }

    @Test
    public void lessThanOrEqualToSemverMatcher() throws InterruptedException {
        assertEquals("off", mSplitClient.getTreatment("semver_less_or_equalto", getVersionAttributesMap("1.22.11")));
        assertEquals("on", mSplitClient.getTreatment("semver_less_or_equalto", getVersionAttributesMap("1.22.9")));
        assertEquals("on", mSplitClient.getTreatment("semver_less_or_equalto", getVersionAttributesMap("1.22.9+build")));
        assertEquals("on", mSplitClient.getTreatment("semver_less_or_equalto", getVersionAttributesMap("1.22.9-rc.0")));
        assertEquals("on", mSplitClient.getTreatment("semver_less_or_equalto", getVersionAttributesMap("1.21.9")));
        assertEquals("off", mSplitClient.getTreatment("semver_less_or_equalto", getVersionAttributesMap(null)));

        assertImpressions(4, 2, 6, "less than or equal to semver");
    }

    @Test
    public void betweenSemverMatcher() throws InterruptedException {
        assertEquals("off", mSplitClient.getTreatment("semver_between", getVersionAttributesMap("2.1.1")));
        assertEquals("on", mSplitClient.getTreatment("semver_between", getVersionAttributesMap("2.1.0+build")));
        assertEquals("on", mSplitClient.getTreatment("semver_between", getVersionAttributesMap("1.25.0")));
        assertEquals("on", mSplitClient.getTreatment("semver_between", getVersionAttributesMap("1.22.9")));
        assertEquals("off", mSplitClient.getTreatment("semver_between", getVersionAttributesMap("1.22.9-rc.0")));
        assertEquals("off", mSplitClient.getTreatment("semver_between", Collections.singletonMap("version", Collections.emptyList())));

        assertImpressions(3, 3, 6, "between semver");
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
        return IntegrationHelper.loadSplitChanges(mContext, "split_changes_semver.json");
    }

    @NonNull
    private static Map<String, Object> getVersionAttributesMap(String value) {
        return Collections.singletonMap("version", value);
    }

    private void assertImpressions(int labelImpressions, int defaultLabelImpressions, int totalImpressions, String defaultLabel) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<ImpressionEntity> impressionEntities = mDatabase.impressionDao().getAll();
        assertEquals(labelImpressions, impressionEntities.stream().map(e -> Json.fromJson(e.getBody(), KeyImpression.class)).filter(e -> e.label.equals(defaultLabel)).count());
        assertEquals(defaultLabelImpressions, impressionEntities.stream().map(e -> Json.fromJson(e.getBody(), KeyImpression.class)).filter(e -> e.label.equals("default rule")).count());
        assertEquals(totalImpressions, mImpressionsOnListener.size());
        assertEquals(totalImpressions, impressionEntities.size());
    }
}
