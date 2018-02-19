package io.split.android.client.impressions;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.utils.Utils;
import timber.log.Timber;

public class ImpressionsManager implements ImpressionListener, Runnable {

    private final SplitClientConfig _config;
    private final CloseableHttpClient _client;
    private final BlockingQueue<KeyImpression> _queue;
    private final ScheduledExecutorService _scheduler;
    private final ImpressionsSender _impressionsSender;

    private final ImpressionsStorageManager _storageManager;

    private long _currentChunkSize = 0;

    private ImpressionsManager(CloseableHttpClient client, SplitClientConfig config, ImpressionsSender impressionsSender, ImpressionsStorageManager impressionsStorageManager) throws URISyntaxException {

        _config = config;
        _client = client;
        _queue = new ArrayBlockingQueue<KeyImpression>(config.impressionsQueueSize());


        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-ImpressionsManager-%d")
                .build();
        _scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
        _scheduler.scheduleAtFixedRate(this, 10, config.impressionsRefreshRate(), TimeUnit.SECONDS);

        _storageManager = impressionsStorageManager;

        if (impressionsSender != null) {
            _impressionsSender = impressionsSender;
        } else {
            _impressionsSender = new HttpImpressionsSender(_client, config.eventsEndpoint(), _storageManager);
        }

    }

    public static ImpressionsManager instance(CloseableHttpClient client,
                                              SplitClientConfig config, ImpressionsStorageManager impressionsStorageManager) throws URISyntaxException {
        return new ImpressionsManager(client, config, null, impressionsStorageManager);
    }

    public static ImpressionsManager instanceForTest(CloseableHttpClient client,
                                                     SplitClientConfig config,
                                                     ImpressionsSender impressionsSender, ImpressionsStorageManager impressionsStorageManager) throws URISyntaxException {
        return new ImpressionsManager(client, config, impressionsSender, impressionsStorageManager);
    }

    @Override
    public void log(Impression impression) {
        try {
            KeyImpression keyImpression = keyImpression(impression);
            if (_queue.offer(keyImpression)) {
                synchronized (this) {
                    accumulateChunkSize(keyImpression);
                    if (_currentChunkSize >= _config.impressionsChunkSize()) {
                        flushImpressions();
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Unable to send impression to ImpressionsManager");
        }

    }

    @Override
    public void close() {
        try {
            _scheduler.shutdown();
            flushImpressions();
            sendImpressions();
            _scheduler.awaitTermination(_config.waitBeforeShutdown(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Timber.e(e, "Unable to close ImpressionsManager");
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
        flushImpressions();
        sendImpressions();
    }

    public void flushImpressions() {
        synchronized (this) {
            if (_queue.remainingCapacity() == 0) {
                Timber.w("Split SDK impressions queue is full. Impressions may have been dropped. Consider increasing capacity.");
            }

            long start = System.currentTimeMillis();

            List<KeyImpression> impressions = new ArrayList<>(_queue.size());
            _queue.drainTo(impressions);

            _currentChunkSize = 0;

                if (impressions != null && !impressions.isEmpty()) {
                    try {
                        _storageManager.storeImpressions(impressions);
                    } catch (IOException e) {
                        Timber.e(e, "Failed to write chunk of impressions %d", impressions.size());
                    }
                }

            if (_config.debugEnabled()) {
                Timber.i("Flushing %d Split impressions took %d millis",
                        impressions.size(), (System.currentTimeMillis() - start));
            }
        }
    }

    private void sendImpressions() {
        long start = System.currentTimeMillis();
        List<StoredImpressions> storedImpressions = _storageManager.getStoredImpressions();
        for(StoredImpressions storedImpression : storedImpressions) {
            boolean succeeded = _impressionsSender.post(storedImpression.impressions());
            if (succeeded) {
                _storageManager.succeededStoredImpression(storedImpression);
            } else {
                _storageManager.failedStoredImpression(storedImpression);
            }
        }
        Timber.d("Posting Split impressions took %d millis", (System.currentTimeMillis() - start));
    }

    private void accumulateChunkSize(KeyImpression keyImpression) {
        long size = Utils.toJsonEntity(keyImpression).getContentLength();
        _currentChunkSize += size;
    }

}
