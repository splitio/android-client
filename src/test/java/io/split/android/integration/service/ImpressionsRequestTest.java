package io.split.android.integration.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.network.HttpClient;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.helpers.FileHelper;
import io.split.sharedtest.fake.HttpClientMock;
import io.split.sharedtest.fake.HttpResponseMock;
import io.split.sharedtest.helper.DatabaseHelper;
import io.split.sharedtest.helper.IntegrationHelper;
import io.split.sharedtest.rules.WorkManagerRule;

@RunWith(RobolectricTestRunner.class)
public class ImpressionsRequestTest {
    @Rule
    public WorkManagerRule mWorkManagerRule = new WorkManagerRule();

    private SplitFactory mSplitFactory;
    private final AtomicReference<String> mImpressionsRequestBody = new AtomicReference<>(null);
    private final CountDownLatch mLatch = new CountDownLatch(1);
    private Context mContext;
    private SplitRoomDatabase mTestDatabase;

    @Before
    public void setUp() throws IOException {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();

        mTestDatabase = DatabaseHelper.getTestDatabase(mContext);
        setUpDatabaseValues(mTestDatabase);
        HttpClient httpClient = new HttpClientMock(IntegrationHelper.buildDispatcher(getMockResponses()));
        mSplitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                new Key("key1"),
                IntegrationHelper.basicConfig(),
                mContext,
                httpClient,
                mTestDatabase
        );

        mImpressionsRequestBody.set("");
    }

    @After
    public void tearDown() {
        if (mTestDatabase != null) {
            mTestDatabase.close();
        }
    }


    private void setUpDatabaseValues(SplitRoomDatabase testDatabase) {
        SplitEntity splitEntity = new SplitEntity();
        splitEntity.setBody(new FileHelper().loadFileContent("split.json"));
        splitEntity.setName("feature_xx");
        splitEntity.setUpdatedAt(System.currentTimeMillis());
        testDatabase.splitDao().insert(Collections.singletonList(splitEntity));
        testDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 9999));
    }

    @Test
    public void verifyResponseObjectContainsDesiredFields() throws InterruptedException {
        SplitClient client = mSplitFactory.client();
        client.getTreatment("feature_xx");
        Thread.sleep(500);
        client.destroy();
        boolean await = mLatch.await(10, TimeUnit.SECONDS);

        JsonObject impressionInnerArray = getImpressionObjectFromResponseBody(mImpressionsRequestBody.get());
        assertTrue(await);
        assertNotNull(impressionInnerArray.get("b"));
        assertNotNull(impressionInnerArray.get("c"));
        assertNotNull(impressionInnerArray.get("k"));
        assertNotNull(impressionInnerArray.get("r"));
        assertNotNull(impressionInnerArray.get("pt"));
        assertNotNull(impressionInnerArray.get("m"));
        assertNotNull(impressionInnerArray.get("t"));
    }

    private JsonObject getImpressionObjectFromResponseBody(String responseBody) {
        return JsonParser
                .parseString(responseBody)
                .getAsJsonArray()
                .get(0)
                .getAsJsonObject()
                .get("i")
                .getAsJsonArray()
                .get(0)
                .getAsJsonObject();
    }

    private Map<String, IntegrationHelper.ResponseClosure> getMockResponses() {
        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();
        responses.put("testImpressions/bulk", (uri, httpMethod, body) -> {
            mImpressionsRequestBody.set(body);
            mLatch.countDown();

            return new HttpResponseMock(200, "{}");
        });

        responses.put("splitChanges", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptySplitChanges(9999, 9999)));
        responses.put(IntegrationHelper.ServicePath.MEMBERSHIPS + "/" + "key1", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptyMySegments()));
        responses.put("v2/auth", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.streamingEnabledToken()));

        return responses;
    }
}
