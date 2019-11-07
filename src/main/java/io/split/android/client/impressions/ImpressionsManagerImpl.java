package io.split.android.client.impressions;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.network.HttpClient;
import io.split.android.client.storage.legacy.ImpressionsStorageManager;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutor;
import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutorImpl;

public class ImpressionsManagerImpl implements ImpressionListener, Runnable, ImpressionsManager {

    private final ImpressionsManagerConfig _config;
    private final BlockingQueue<KeyImpression> _queue;
    private final PausableScheduledThreadPoolExecutor _scheduler;
    private final ImpressionsSender _impressionsSender;

    private final ImpressionsStorageManager _storageManager;

    private long _currentChunkSize = 0;

    private ImpressionsManagerImpl(HttpClient client, ImpressionsManagerConfig config, ImpressionsSender impressionsSender, ImpressionsStorageManager impressionsStorageManager) throws URISyntaxException {

        _config = config;
        _queue = new ArrayBlockingQueue<KeyImpression>(config.queueSize());


        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-ImpressionsManager-%d")
                .build();
        _scheduler = PausableScheduledThreadPoolExecutorImpl.newSingleThreadScheduledExecutor(threadFactory);
        _scheduler.scheduleAtFixedRate(this, 10, config.refreshRate(), TimeUnit.SECONDS);

        _storageManager = impressionsStorageManager;

        if (impressionsSender != null) {
            _impressionsSender = impressionsSender;
        } else {
            _impressionsSender = new HttpImpressionsSender(client, new URI(config.endpoint()), _storageManager);
        }
    }

    public static ImpressionsManagerImpl instance(HttpClient client,
                                                  ImpressionsManagerConfig config, ImpressionsStorageManager impressionsStorageManager) throws URISyntaxException {
        return new ImpressionsManagerImpl(client, config, null, impressionsStorageManager);
    }

    public static ImpressionsManagerImpl instanceForTest(HttpClient client,
                                                         ImpressionsManagerConfig config,
                                                         ImpressionsSender impressionsSender, ImpressionsStorageManager impressionsStorageManager) throws URISyntaxException {
        return new ImpressionsManagerImpl(client, config, impressionsSender, impressionsStorageManager);
    }

    @Override
    public void log(Impression impression) {
        try {
            KeyImpression keyImpression = keyImpression(impression);
            if (_queue.offer(keyImpression)) {
                synchronized (this) {
                    accumulateChunkSize(keyImpression);
                    if (_currentChunkSize >= _config.chunkSize()) {
                        flushImpressions();
                    }
                }
            }
        } catch (Exception e) {
            Logger.e(e, "Unable to send impression to ImpressionsManager");
        }

    }

    @Override
    public void close() {
        try {
            _scheduler.shutdown();
            flushImpressions();
            sendImpressions();
            _storageManager.close();
            _scheduler.awaitTermination(_config.waitBeforeShutdown(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Logger.e(e, "Unable to close ImpressionsManager");
        }

    }

    @Override
    public void pause() {
        if (_scheduler != null) {
            _scheduler.pause();
        }
    }

    @Override
    public void resume() {
        if (_scheduler != null) {
            _scheduler.resume();
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

    @Override
    public void flushImpressions() {
        synchronized (this) {
            if (_queue.remainingCapacity() == 0) {
                Logger.w("Split SDK impressions queue is full. Impressions may have been dropped. Consider increasing capacity.");
            }

            long start = System.currentTimeMillis();

            List<KeyImpression> impressions = new ArrayList<>(_queue.size());
            _queue.drainTo(impressions);

            _currentChunkSize = 0;

                if (impressions != null && !impressions.isEmpty()) {
                    try {
                        _storageManager.storeImpressions(impressions);
                    } catch (IOException e) {
                        Logger.e(e, "Failed to write chunk of impressions %d", impressions.size());
                    }
                }


                Logger.d("Flushing %d Split impressions took %d millis",
                        impressions.size(), (System.currentTimeMillis() - start));

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

        Logger.d("Posting Split impressions took %d millis", (System.currentTimeMillis() - start));
    }

    private void accumulateChunkSize(KeyImpression keyImpression) {
        String data = Json.toJson(keyImpression);
        if(data != null) {
            _currentChunkSize += data.getBytes().length;
        }
    }

    @Override
    public void saveToDisk() {
        flushImpressions();
        _storageManager.saveToDisk();
    }
}
