package tests.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import helper.IntegrationHelper;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.service.synchronizer.ThreadUtils;
import io.split.android.client.utils.logger.SplitLogLevel;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import tests.integration.shared.TestingHelper;

public class ProxyFactoryTest {

    @Test
    public void initializeWithProxy() throws InterruptedException {
        final AtomicBoolean crashes = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);
        ThreadUtils.runInMainThread(() -> {
            try {
                SplitFactoryBuilder.build(IntegrationHelper.dummyApiKey(),
                        IntegrationHelper.dummyUserKey(),
                        new SplitClientConfig.Builder()
                                .proxyHost("https://custom_host.com")
                                .build(),
                        InstrumentationRegistry.getInstrumentation().getContext());
            } catch (Exception e) {
                crashes.set(true);
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertFalse(crashes.get());
    }

    @Test
    public void settingProxyReplacesAllUrls() throws IOException, URISyntaxException, InterruptedException, TimeoutException {

        MockWebServer proxyServer = new MockWebServer();
        proxyServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getRequestLine().contains("/mySegments")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}");
                } else if (request.getRequestLine().contains("splitChanges")) {
                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.emptySplitChanges(-1));
                } else {
                    return new MockResponse().setResponseCode(200).setBody("{}");
                }
            }
        });
        proxyServer.start();

        MockWebServer baseServer = new MockWebServer();
        baseServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(200);
            }
        });
        baseServer.start();

        String url = baseServer.url("").toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(url).eventsEndpoint(url).build();
        Key key = IntegrationHelper.dummyUserKey();
        SplitFactory factory = SplitFactoryBuilder.build(IntegrationHelper.dummyApiKey(),
                key,
                new SplitClientConfig.Builder()
                        .serviceEndpoints(endpoints)
                        .proxyHost(proxyServer.url("").toString())
                        .logLevel(SplitLogLevel.VERBOSE)
                        .build(),
                InstrumentationRegistry.getInstrumentation().getContext());

        SplitClient client = factory.client(key);
        CountDownLatch readyLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask readyTask = new TestingHelper.TestEventTask(readyLatch);
        client.on(SplitEvent.SDK_READY, readyTask);

        boolean await = readyLatch.await(2, TimeUnit.SECONDS);
        assertTrue(await);
        proxyServer.shutdown();
        baseServer.shutdown();
    }

}
