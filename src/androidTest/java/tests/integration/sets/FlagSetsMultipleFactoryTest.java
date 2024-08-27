package tests.integration.sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static helper.IntegrationHelper.ResponseClosure.getSinceFromUri;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import helper.FileHelper;
import helper.IntegrationHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFilter;
import io.split.android.client.SyncConfig;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.utils.Json;
import tests.integration.shared.TestingHelper;

public class FlagSetsMultipleFactoryTest {
    private final FileHelper mFileHelper = new FileHelper();
    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    public void factoriesContainCorrectSplits() throws IOException, InterruptedException {
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch readyLatch2 = new CountDownLatch(1);
        HttpClientMock httpClient = new HttpClientMock(getDispatcher(2));

        SplitClientConfig.Builder builderWithoutPrefix = SplitClientConfig.builder().syncConfig(SyncConfig.builder()
                .addSplitFilter(SplitFilter.bySet(Arrays.asList("set_1", "set_2")))
                .build());
        SplitClientConfig.Builder builderWithPrefix = SplitClientConfig.builder().prefix("mydb").syncConfig(SyncConfig.builder()
                .addSplitFilter(SplitFilter.bySet(Collections.singletonList("set_3")))
                .build());

        Thread thread = buildFactoryInitializationThread(builderWithoutPrefix, httpClient, readyLatch);
        Thread thread2 = buildFactoryInitializationThread(builderWithPrefix, httpClient, readyLatch2);

        thread.start();
        thread2.start();

        boolean readyAwait = readyLatch.await(5, TimeUnit.SECONDS);
        boolean readyAwait2 = readyLatch2.await(5, TimeUnit.SECONDS);

        assertTrue(readyAwait);
        assertTrue(readyAwait2);

        List<String> namesInDb = getNamesFromDb("sdk_ey_1");
        List<String> namesInDb2 = getNamesFromDb("mydbsdk_ey_1");

        assertEquals(1, namesInDb.size());
        assertEquals(1, namesInDb2.size());
        assertEquals("workm", namesInDb.get(0));
        assertEquals("workm_set_3", namesInDb2.get(0));
    }

    @NonNull
    private Thread buildFactoryInitializationThread(SplitClientConfig.Builder builder,HttpClientMock httpClient, CountDownLatch readyLatch) {
        return new Thread(() -> {
            CountDownLatch innerLatch = new CountDownLatch(1);
            try {
                initSplitFactory(builder, httpClient, innerLatch);
                innerLatch.await(5, TimeUnit.SECONDS);
                readyLatch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void initSplitFactory(SplitClientConfig.Builder builder, HttpClientMock httpClient, CountDownLatch innerLatch) {
        SplitFactory factory = IntegrationHelper.buildFactory(
                "sdk_key_1",
                IntegrationHelper.dummyUserKey(),
                builder.build(),
                mContext,
                httpClient);

        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(innerLatch));
    }

    @NonNull
    private List<String> getNamesFromDb(String dbName) {
        List<String> namesInDb = new ArrayList<>();
        try (SQLiteDatabase sqLiteDatabase = mContext.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)) {
            try (Cursor cursor = sqLiteDatabase.query("splits", null, null, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        namesInDb.add(cursor.getString(cursor.getColumnIndex("name")));
                    } while (cursor.moveToNext());
                }
            }
        }
        return namesInDb;
    }

    private String loadSplitChangeWithSet(int setsCount) {
        String change = mFileHelper.loadFileContent(mContext, "split_changes_flag_set-" + setsCount + ".json");
        SplitChange parsedChange = Json.fromJson(change, SplitChange.class);
        parsedChange.since = parsedChange.till;

        return Json.toJson(parsedChange);
    }

    private HttpResponseMockDispatcher getDispatcher(int setsCount) {
        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();
        responses.put("splitChanges", (uri, httpMethod, body) -> {
            String since = getSinceFromUri(uri);
            if (since.equals("-1")) {
                return new HttpResponseMock(200, loadSplitChangeWithSet(setsCount));
            } else {
                return new HttpResponseMock(200, IntegrationHelper.emptySplitChanges(1602796638344L, 1602796638344L));
            }
        });

        responses.put(IntegrationHelper.ServicePath.MEMBERSHIPS + "/" + "/CUSTOMER_ID", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptyMySegments()));

        return IntegrationHelper.buildDispatcher(responses);
    }
}
