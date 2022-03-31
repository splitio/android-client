package tests.integration.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class SharedClientsIntegrationTest {

    private Context mContext;
    private ServerMock mWebServer;
    private SplitRoomDatabase mRoomDb;
    private SplitFactory mSplitFactory;
    private List<String> mJsonChanges = null;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 10));
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.DATBASE_MIGRATION_STATUS, 1));

        if (mJsonChanges == null) {
            loadSplitChanges();
        }

        mWebServer = new ServerMock(mJsonChanges);

        String serverUrl = mWebServer.getServerUrl();
        mSplitFactory = getFactory(serverUrl);

        mRoomDb.clearAllTables();
    }

    private SplitFactory getFactory(String serverUrl) {
        return IntegrationHelper.buildFactory(IntegrationHelper.dummyApiKey(),
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
                        .streamingEnabled(true)
                        .build(),
                mContext,
                null,
                mRoomDb);
    }

    @After
    public void tearDown() {
        mSplitFactory.destroy();
    }

    @Test
    public void multipleClientsAreReadyFromCache() throws InterruptedException {
        insertSplitsIntoDb();
        verifyEventExecution(SplitEvent.SDK_READY_FROM_CACHE);
    }

    @Test
    public void multipleClientsAreReady() throws InterruptedException {
        insertSplitsIntoDb();
        verifyEventExecution(SplitEvent.SDK_READY);
    }

    private void verifyEventExecution(SplitEvent event) throws InterruptedException {
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch readyLatch2 = new CountDownLatch(1);

        AtomicInteger readyCount = new AtomicInteger(0);
        AtomicInteger readyCount2 = new AtomicInteger(0);

        SplitClient client = mSplitFactory.client();
        SplitClient client2 = mSplitFactory.client(new Key("key2"));

        client.on(event, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                readyCount.addAndGet(1);
                readyLatch.countDown();
            }
        });

        client2.on(event, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                readyCount2.addAndGet(1);
                readyLatch2.countDown();
            }
        });

        boolean await = readyLatch.await(5, TimeUnit.SECONDS);
        boolean await2 = readyLatch2.await(5, TimeUnit.SECONDS);

        assertTrue(await);
        assertTrue(await2);
        assertEquals(1, readyCount.get());
        assertEquals(1, readyCount2.get());
    }

    private void loadSplitChanges() {
        FileHelper fileHelper = new FileHelper();
        mJsonChanges = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            String changes = fileHelper.loadFileContent(mContext, "split_changes_" + (i + 1) + ".json");
            mJsonChanges.add(changes);
        }
    }

    private void insertSplitsIntoDb() {
        SplitChange change = Json.fromJson(mJsonChanges.get(0), SplitChange.class);
        List<SplitEntity> entities = new ArrayList<>();
        for (Split split : change.splits) {
            String splitName = split.name;
            SplitEntity entity = new SplitEntity();
            entity.setName(splitName);
            entity.setBody(Json.toJson(split));
            entities.add(entity);
        }
        mRoomDb.splitDao().insert(entities);
    }
}
