package tests.integration.shared;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;

import helper.DatabaseHelper;
import helper.IntegrationHelper;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.storage.db.SplitRoomDatabase;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockWebServer;

abstract class BaseSharedClientsTest {

    protected Context mContext;
    protected MockWebServer mWebServer;
    protected SplitRoomDatabase mRoomDb;
    protected SplitFactory mSplitFactory;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);

        mWebServer = new MockWebServer();

        String serverUrl = mWebServer.url("/").toString();
        Dispatcher dispatcher = getDispatcher();
        mWebServer.setDispatcher(dispatcher);
        mSplitFactory = IntegrationHelper.buildFactory(IntegrationHelper.dummyApiKey(),
                new Key("key1"),
                getDefaultConfig(serverUrl),
                mContext,
                null,
                mRoomDb);

        mRoomDb.clearAllTables();
    }

    @After
    public void tearDown() {
        mSplitFactory.destroy();
    }

    @NonNull
    protected SplitClientConfig getDefaultConfig(String serverUrl) {
        return SplitClientConfig.builder()
                .serviceEndpoints(ServiceEndpoints.builder()
                        .apiEndpoint(serverUrl).eventsEndpoint(serverUrl).build())
                .ready(30000)
                .enableDebug()
                .featuresRefreshRate(99999)
                .segmentsRefreshRate(99999)
                .impressionsRefreshRate(99999)
                .trafficType("account")
                .streamingEnabled(true)
                .build();
    }

    protected abstract Dispatcher getDispatcher();
}
