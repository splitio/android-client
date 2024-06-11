package tests.integration.pin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.network.CertificatePinningConfiguration;
import io.split.android.client.network.DevelopmentSslConfig;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;

public class CertPinningTest {

    private MockWebServer mWebServer;
    private HeldCertificate mHeldCertificate;
    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private HandshakeCertificates mClientCertificates;
    private HandshakeCertificates mServerCertificates;
    private Map<String, AtomicInteger> mEndpointHits;

    @Before
    public void setUp() throws IOException {
        mEndpointHits = new HashMap<>();

        mHeldCertificate = new HeldCertificate.Builder()
                .commonName("localhost")
                .build();
        mServerCertificates = new HandshakeCertificates.Builder()
                .heldCertificate(mHeldCertificate)
                .build();
        mClientCertificates = new HandshakeCertificates.Builder()
                .addTrustedCertificate(mHeldCertificate.certificate())
                .build();

        mWebServer = new MockWebServer();
        mWebServer.useHttps(mServerCertificates.sslSocketFactory(), false);
        mWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                System.out.println("Receiving request to " + request.getRequestUrl().toString());

                if (request.getHandshake() == null) {
                    return new MockResponse().setResponseCode(500);
                }

                if (request.getRequestUrl().encodedPathSegments().contains("splitChanges")) {
                    if (mEndpointHits.containsKey("splitChanges")) {
                        mEndpointHits.get("splitChanges").getAndIncrement();
                    } else {
                        mEndpointHits.put("splitChanges", new AtomicInteger(1));
                    }
                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.emptySplitChanges(1602796638344L, 1602796638344L));
                } else if (request.getRequestUrl().encodedPathSegments().contains("mySegments")) {
                    if (mEndpointHits.containsKey("mySegments")) {
                        mEndpointHits.get("mySegments").getAndIncrement();
                    } else {
                        mEndpointHits.put("mySegments", new AtomicInteger(1));
                    }
                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.emptyMySegments());
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        });
        mWebServer.start();
    }

    @Test
    public void certPinningWithWrongHashFailsBasicTest() throws InterruptedException {
        CountDownLatch failureLatch = new CountDownLatch(2); // 2 counts, one for splitChanges and one for mySegments
        SplitClientConfig config = getConfig(CertificatePinningConfiguration.builder()
                .addPin("localhost", "sha256/as56fasf56")
                .addPin("localhost", "sha1/bbb6215asfee")
                .failureListener((host, certificateChain) -> failureLatch.countDown())
                .build());

        SplitFactory factory = getFactory(config);

        CountDownLatch latch = new CountDownLatch(1);
        factory.client().on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                latch.countDown();
            }
        });
        boolean await = latch.await(5, TimeUnit.SECONDS);
        boolean failureAwait = failureLatch.await(5, TimeUnit.SECONDS);

        assertFalse(await);
        assertTrue(failureAwait);
    }

    @Test
    public void certPinningIsSuccessfulBasicTest() throws InterruptedException {
        CountDownLatch failureLatch = new CountDownLatch(2); // 2 counts, one for splitChanges and one for mySegments
        SplitClientConfig config = getConfig(CertificatePinningConfiguration.builder()
                .addPin("localhost", "sha256/" + sha256(mHeldCertificate.certificate().getPublicKey().getEncoded()))
                .failureListener((host, certificateChain) -> failureLatch.countDown())
                .build());

        SplitFactory factory = getFactory(config);

        CountDownLatch latch = new CountDownLatch(1);
        factory.client().on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                latch.countDown();
            }
        });
        boolean await = latch.await(5, TimeUnit.SECONDS);
        boolean failureAwait = failureLatch.await(5, TimeUnit.SECONDS);

        assertTrue(await);
        assertFalse(failureAwait);
    }

    private String sha256(byte[] encoded) {
        try {
            return IntegrationHelper.sha256(encoded);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private SplitFactory getFactory(SplitClientConfig config) {
        return IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                IntegrationHelper.dummyUserKey(),
                config,
                mContext,
                null,
                null,
                null,
                null,
                null);
    }

    private SplitClientConfig getConfig(CertificatePinningConfiguration certPinningConfig) {
        return new TestableSplitConfigBuilder()
                .ready(10)
                .trafficType("user")
                .enableDebug()
                .streamingEnabled(false)
                .serviceEndpoints(ServiceEndpoints.builder()
                        .apiEndpoint(mWebServer.url("/api").toString())
                        .eventsEndpoint(mWebServer.url("/api").toString())
                        .build())
                .certificatePinningConfiguration(certPinningConfig)
                .developmentSslConfig(new DevelopmentSslConfig(mClientCertificates.trustManager(), (hostname, session) -> true))
                .build();
    }
}
