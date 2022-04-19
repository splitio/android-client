package tests.integration;

import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import helper.TestingHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Logger;
import io.split.sharedtest.fake.HttpStreamResponseMock;

public class SplitChangesCdnBypassTest {

    private final static String MSG_SPLIT_UPDATE = "push_msg-split_update.txt";

    private BlockingQueue<String> mStreamingData;
    private CountDownLatch mSseLatch;
    private CountDownLatch mBypassLatch;
    private SplitFactory mSplitFactory;
    private Context mContext;

    @Before
    public void setup() throws IOException {
        mStreamingData = new LinkedBlockingDeque<>();
        mBypassLatch = new CountDownLatch(1);
        mSseLatch = new CountDownLatch(1);

        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        SplitRoomDatabase splitRoomDatabase = DatabaseHelper.getTestDatabase(mContext);
        splitRoomDatabase.clearAllTables();
        splitRoomDatabase.generalInfoDao().update(
                new GeneralInfoEntity(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP, System.currentTimeMillis() / 1000 - 30));
        mSplitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                IntegrationHelper.dummyUserKey(),
                IntegrationHelper.basicConfig(),
                mContext,
                new HttpClientMock(buildDispatcher()),
                splitRoomDatabase);
    }

    @Test
    public void tillParameterIsEventuallyAdded() throws Exception {
        SplitClient client = mSplitFactory.client();

        CountDownLatch latch = new CountDownLatch(1);
        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);
        SplitEventTaskHelper readyTimeOutTask = new SplitEventTaskHelper(latch);

        client.on(SplitEvent.SDK_READY, readyTask);
        client.on(SplitEvent.SDK_READY_TIMED_OUT, readyTimeOutTask);

        latch.await(20, TimeUnit.SECONDS);
        mSseLatch.await(20, TimeUnit.SECONDS);

        TestingHelper.pushKeepAlive(mStreamingData);
        TestingHelper.delay(500);

        pushSplitsUpdateMessage();
        boolean await = mBypassLatch.await(300, TimeUnit.SECONDS);
        assertTrue(await);

        client.destroy();
    }

    @NonNull
    private HttpResponseMockDispatcher buildDispatcher() {
        return new HttpResponseMockDispatcher() {

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    Logger.i("** SSE Connect hit");
                    mSseLatch.countDown();
                    return createStreamResponse(200, mStreamingData);
                } catch (Exception e) {
                    Logger.e("** SSE Connect error: " + e.getLocalizedMessage());
                }
                return null;
            }

            @Override

            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("/mySegments")) {
                    return new HttpResponseMock(200, "{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, " +
                            "{ \"id\":\"id1\", \"name\":\"segment2\"}]}");

                } else if (uri.getPath().contains("/splitChanges")) {
                    if (uri.getQuery().contains("till=3") && uri.getQuery().contains("since=3")) {
                        return getSplitsMockResponse("3", "4");
                    }

                    if (uri.getQuery().contains("till")) {
                        mBypassLatch.countDown();
                    }

                    if (uri.getQuery().contains("since=-1")) {
                        return getSplitsMockResponse("-1", "2");
                    } else if (uri.getQuery().contains("since=2")) {
                        return getSplitsMockResponse("2", "3");
                    } else if (uri.getQuery().contains("since=3")) {
                        return getSplitsMockResponse("3", "3");
                    }
                    return new HttpResponseMock(200, "{\"splits\":[], \"since\": 4, \"till\": 4 }");
                } else if (uri.getPath().contains("/testImpressions/bulk")) {
                    return new HttpResponseMock(200);
                } else if (uri.getPath().contains("/auth")) {
                    Logger.i("** SSE Auth hit");
                    return new HttpResponseMock(200, IntegrationHelper.streamingEnabledToken());
                } else {
                    return new HttpResponseMock(404);
                }
            }
        };
    }

    @NonNull
    private HttpResponseMock getSplitsMockResponse(final String since, final String till) {
        return new HttpResponseMock(200, "{\"splits\":[], \"since\": " + since + ", \"till\": " + till + " }");
    }

    private HttpStreamResponseMock createStreamResponse(int status, BlockingQueue<String> streamingResponseData) throws IOException {
        return new HttpStreamResponseMock(status, streamingResponseData);
    }

    private void pushSplitsUpdateMessage() {
        String message = new FileHelper().loadFileContent(mContext, MSG_SPLIT_UPDATE);
        message = message.replace("$TIMESTAMP$", String.valueOf(System.currentTimeMillis()));
        message = message.replace("1000100", "4");
        try {
            mStreamingData.put(message + "" + "\n");

            Logger.d("Pushed message: " + message);
        } catch (InterruptedException e) {
        }
    }
}
