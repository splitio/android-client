import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import helper.SplitEventTaskHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.utils.Logger;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class InitialChangeNumberTest {

    Context mContext;
    MockWebServer mWebServer;
    long mFirstChangeNumberReceived;
    boolean mIsFirstChangeNumber = true;
    final long INITIAL_CHANGE_NUMBER = 1568396481;

    @Before
    public void setup() {
        setupServer();
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mFirstChangeNumberReceived = -1;
        mIsFirstChangeNumber = true;
    }

    @After
    public void tearDown() throws IOException {
        mWebServer.shutdown();
    }

    private void setupServer() {
        mWebServer = new MockWebServer();

        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch (RecordedRequest request) throws InterruptedException {
                if (request.getPath().contains("/mySegments")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}");
                } else if (request.getPath().contains("/splitChanges")) {

                    long changeNumber = -1;
                    if(mIsFirstChangeNumber) {
                        String path = request.getPath();
                        changeNumber = Long.valueOf(path.substring(path.indexOf("=") + 1));
                        mFirstChangeNumberReceived = changeNumber;
                        mIsFirstChangeNumber = false;
                    }
                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"splits\":[], \"since\":" + changeNumber + ", \"till\":" + (changeNumber + 1000) + "}");
                } else if (request.getPath().contains("/events/bulk")) {
                    String trackRequestBody = request.getBody().readUtf8();

                    return new MockResponse().setResponseCode(200);
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };
        mWebServer.setDispatcher(dispatcher);
    }

    @Test
    public void firstRequestChangeNumber() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        String apiKey = "99049fd8653247c5ea42bc3c1ae2c6a42bc3";
        String dataFolderName = "2a1099049fd8653247c5ea42bOIajMRhH0R0FcBwJZM4ca7zj6HAq1ZDS";
        File cacheDir = mContext.getCacheDir();

        File dataFolder = new File(cacheDir, dataFolderName);
        if(dataFolder.exists()) {
            File[] files = dataFolder.listFiles();
            if(files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            boolean isDataFolderDelete = dataFolder.delete();
            log("Data folder exists and deleted: " + isDataFolderDelete);
        }
        dataFolder.mkdir();


        for(int i=0; i<10; i++) {
            String splitName = "feature_" + i;
            long changeNumber = INITIAL_CHANGE_NUMBER - i * 100;
            String jsonSplit = String.format("{\"name\":\"%s\", \"changeNumber\": %d}",
                            splitName, changeNumber);
            write(dataFolder, splitName, jsonSplit);
        }

        SplitClient client;

        final String url = mWebServer.url("/").url().toString();

        Key key = new Key("CUSTOMER_ID",null);
        SplitClientConfig config = SplitClientConfig.builder()
                .endpoint(url, url)
                .ready(30000)
                .featuresRefreshRate(30)
                .segmentsRefreshRate(30)
                .impressionsRefreshRate(99999)
                .enableDebug()
                .build();


        SplitFactory splitFactory = SplitFactoryBuilder.build(apiKey, key, config, mContext);

        client = splitFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);
        SplitEventTaskHelper readyTimeOutTask = new SplitEventTaskHelper(latch);

        client.on(SplitEvent.SDK_READY, readyTask);
        client.on(SplitEvent.SDK_READY_TIMED_OUT, readyTimeOutTask);

        latch.await(40, TimeUnit.SECONDS);

        Assert.assertTrue(readyTask.isOnPostExecutionCalled);
        Assert.assertEquals(INITIAL_CHANGE_NUMBER, mFirstChangeNumberReceived); // Checks that change number is the bigger number from cached splits

    }


    private void write(File folder, String splitName, String content) throws IOException {
        File file = new File(folder, "SPLITIO.split." + splitName);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(content.getBytes());
        } catch (FileNotFoundException e) {
            Logger.e(e, "Failed to write content");
            throw e;
        } catch (IOException e) {
            Logger.e(e, "Failed to write content");
            throw e;
        } finally {
            try {
                if(fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                Logger.e(e, "Failed to close file");
            }
        }
    }

    private void log(String m) {
        System.out.println("FACTORY_TEST: " + m);
    }

}
