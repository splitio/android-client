package io.split.android.client.perf;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.platform.app.InstrumentationRegistry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import helper.TestableSplitConfigBuilder;
import io.split.android.client.PerfSeedSpec;
import io.split.android.client.PerfSeedSpec.Spec;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.api.Key;
import io.split.android.client.events.SdkReadyMetadata;
import io.split.android.client.events.SplitEventListener;
import okhttp3.mockwebserver.MockWebServer;

/**
 * One SDK launch against a stub server: builds the factory on the main thread, times the build and
 * the two ready events, and checks that what was measured is the cache path.
 */
final class PerfLaunch {

    private static final long EVENT_TIMEOUT_SECONDS = 120;
    private static final int REFRESH_SECONDS = 3600;

    private PerfLaunch() {
    }

    /** What a launch measured. Times are nanoseconds since the call to {@code build()} began. */
    static final class Result {
        final SplitFactory factory;
        final long buildNs;
        final long readyFromCacheNs;
        final long readyNs;
        final String readyFromCachePath;
        final Long firstFlagsSince;
        final int splitNames;

        Result(SplitFactory factory, long buildNs, long readyFromCacheNs, long readyNs,
               String readyFromCachePath, Long firstFlagsSince, int splitNames) {
            this.factory = factory;
            this.buildNs = buildNs;
            this.readyFromCacheNs = readyFromCacheNs;
            this.readyNs = readyNs;
            this.readyFromCachePath = readyFromCachePath;
            this.firstFlagsSince = firstFlagsSince;
            this.splitNames = splitNames;
        }
    }

    /** Polling only, every endpoint on the stub, nothing scheduled to fire during a launch. */
    static SplitClientConfig config(MockWebServer server, Spec spec) {
        String api = server.url("/api/").toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(api)
                .eventsEndpoint(api)
                .telemetryServiceEndpoint(api)
                .sseAuthServiceEndpoint(api)
                .streamingServiceEndpoint(server.url("/sse").toString())
                .build();
        return new TestableSplitConfigBuilder()
                .serviceEndpoints(endpoints)
                .encryptionEnabled(spec.encrypted)
                .streamingEnabled(false)
                .featuresRefreshRate(REFRESH_SECONDS)
                .segmentsRefreshRate(REFRESH_SECONDS)
                .impressionsRefreshRate(REFRESH_SECONDS)
                .eventFlushInterval(REFRESH_SECONDS)
                .shouldRecordTelemetry(false)
                .build();
    }

    /** The caller destroys {@link Result#factory}. */
    static Result run(Context context, SplitClientConfig config, int expectedFlags, StubDispatcher stub) {
        final CountDownLatch cacheReady = new CountDownLatch(1);
        final CountDownLatch ready = new CountDownLatch(1);
        final AtomicReference<Long> cacheReadyAt = new AtomicReference<>();
        final AtomicReference<Long> readyAt = new AtomicReference<>();
        final AtomicReference<String> path = new AtomicReference<>();
        final AtomicReference<SplitFactory> factoryRef = new AtomicReference<>();
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final long[] build = new long[2];

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                try {
                    build[0] = SystemClock.elapsedRealtimeNanos();
                    SplitFactory factory = SplitFactoryBuilder.build(PerfSeedSpec.SDK_KEY,
                            new Key(PerfSeedSpec.USER_KEY), config, context);
                    build[1] = SystemClock.elapsedRealtimeNanos();
                    factoryRef.set(factory);
                    factory.client().addEventListener(new SplitEventListener() {
                        @Override
                        public void onReadyFromCache(SplitClient client, SdkReadyMetadata metadata) {
                            cacheReadyAt.set(SystemClock.elapsedRealtimeNanos());
                            path.set(pathOf(metadata));
                            cacheReady.countDown();
                        }

                        @Override
                        public void onReady(SplitClient client, SdkReadyMetadata metadata) {
                            readyAt.set(SystemClock.elapsedRealtimeNanos());
                            ready.countDown();
                        }
                    });
                } catch (Throwable t) {
                    failure.set(t);
                }
            }
        });
        if (failure.get() != null) {
            throw new AssertionError("build() failed: " + failure.get(), failure.get());
        }

        SplitFactory factory = factoryRef.get();
        int splitNames;
        Long firstSince;
        try {
            await(cacheReady, "SDK_READY_FROM_CACHE");
            await(ready, "SDK_READY");
            splitNames = factory.manager().splitNames().size();
            firstSince = stub.firstFlagsSince();
            PerfLaunchGuards.requireSplitNames(expectedFlags, splitNames);
            PerfLaunchGuards.requireCachePath(path.get());
            PerfLaunchGuards.requireFirstSince(firstSince, PerfSeedSpec.FLAGS_CN);
        } catch (RuntimeException | Error e) {
            factory.destroy();
            throw e;
        }
        return new Result(factory, build[1] - build[0], cacheReadyAt.get() - build[0],
                readyAt.get() - build[0], path.get(), firstSince, splitNames);
    }

    private static String pathOf(SdkReadyMetadata metadata) {
        Boolean initialCacheLoad = metadata == null ? null : metadata.isInitialCacheLoad();
        if (initialCacheLoad == null) {
            return "replay";
        }
        return initialCacheLoad ? "network" : "cache";
    }

    private static void await(CountDownLatch latch, String event) {
        try {
            if (!latch.await(EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionError(event + " did not arrive within " + EVENT_TIMEOUT_SECONDS + " s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted waiting for " + event, e);
        }
    }
}
