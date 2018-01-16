package io.split.android.client.impressions;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.TestImpressions;

import org.apache.http.impl.client.CloseableHttpClient;

import timber.log.Timber;


import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by patricioe on 6/17/16.
 */
public class ImpressionsManager implements ImpressionListener, Runnable {

    private final SplitClientConfig _config;
    private final CloseableHttpClient _client;
    private final BlockingQueue<KeyImpression> _queue;
    private final ScheduledExecutorService _scheduler;
    private final ImpressionsSender _impressionsSender;

    public static ImpressionsManager instance(CloseableHttpClient client,
                                              SplitClientConfig config) throws URISyntaxException {
        return new ImpressionsManager(client, config, null);
    }

    public static ImpressionsManager instanceForTest(CloseableHttpClient client,
                                                     SplitClientConfig config,
                                                     ImpressionsSender impressionsSender) throws URISyntaxException {
        return new ImpressionsManager(client, config, impressionsSender);
    }

    private ImpressionsManager(CloseableHttpClient client, SplitClientConfig config, ImpressionsSender impressionsSender) throws URISyntaxException {

        _config = config;
        _client = client;
        _queue = new ArrayBlockingQueue<KeyImpression>(config.impressionsQueueSize());

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-ImpressionsManager-%d")
                .build();
        _scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
        _scheduler.scheduleAtFixedRate(this, 10, config.impressionsRefreshRate(), TimeUnit.SECONDS);

        if (impressionsSender != null) {
            _impressionsSender = impressionsSender;
        } else {
            _impressionsSender = new HttpImpressionsSender(_client, config.eventsEndpoint());
        }
    }

    @Override
    public void log(Impression impression) {
        try {
            KeyImpression keyImpression = keyImpression(impression);
            _queue.offer(keyImpression);
        } catch (Exception e) {
            Timber.w(e, "Unable to send impression to ImpressionsManager");
        }

    }

    @Override
    public void close() {
        try {
            _scheduler.shutdown();
            sendImpressions();
            _scheduler.awaitTermination(_config.waitBeforeShutdown(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Timber.w(e, "Unable to close ImpressionsManager");
        }

    }

    private KeyImpression keyImpression(Impression impression) {
        KeyImpression result = new KeyImpression();
        result.feature = impression.split();
        result.keyName = impression.key();
        result.bucketingKey = impression.bucketingKey();
        result.label = impression.appliedRule();
        result.treatment = impression.treatment();
        result.time = impression.time();
        result.changeNumber = impression.changeNumber();
        return result;
    }

    @Override
    public void run() {
        sendImpressions();
    }

    private void sendImpressions() {

        if (_queue.remainingCapacity() == 0) {
            Timber.w("Split SDK impressions queue is full. Impressions may have been dropped. Consider increasing capacity.");
        }

        long start = System.currentTimeMillis();

        List<KeyImpression> impressions = new ArrayList<>(_queue.size());
        _queue.drainTo(impressions);

        if (impressions.isEmpty()) {
            return; // Nothing to send
        }

        Map<String, List<KeyImpression>> tests = new HashMap<>();

        for (KeyImpression ki : impressions) {
            List<KeyImpression> impressionsForTest = tests.get(ki.feature);
            if (impressionsForTest == null) {
                impressionsForTest = new ArrayList<>();
                tests.put(ki.feature, impressionsForTest);
            }
            impressionsForTest.add(ki);
        }

        List<TestImpressions> toShip = Lists.newArrayList();

        for (Map.Entry<String, List<KeyImpression>> entry : tests.entrySet()) {
            String testName = entry.getKey();
            List<KeyImpression> keyImpressions = entry.getValue();

            TestImpressions testImpressionsDTO = new TestImpressions();
            testImpressionsDTO.testName = testName;
            testImpressionsDTO.keyImpressions = keyImpressions;

            toShip.add(testImpressionsDTO);
        }

        _impressionsSender.post(toShip);

        if (_config.debugEnabled()) {
            Timber.i("Posting %d Split impressions took %d millis",
                    impressions.size(), (System.currentTimeMillis() - start));
        }
    }

}
