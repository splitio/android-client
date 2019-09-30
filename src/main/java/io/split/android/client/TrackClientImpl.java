package io.split.android.client;

import android.annotation.SuppressLint;
import android.support.annotation.VisibleForTesting;

import com.google.common.collect.Lists;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.split.android.client.cache.ISplitCache;
import io.split.android.client.dtos.Event;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.track.EventsChunk;
import io.split.android.client.track.TrackClientConfig;
import io.split.android.client.track.TrackStorageManager;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.Utils;
import io.split.android.client.validators.ValidationMessageLoggerImpl;
import io.split.android.client.validators.ValidationConfig;
import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutor;
import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutorImpl;
import io.split.android.engine.scheduler.PausableThreadPoolExecutor;
import io.split.android.engine.scheduler.PausableThreadPoolExecutorImpl;

import static java.lang.Thread.MIN_PRIORITY;


public class TrackClientImpl implements TrackClient {

    //Events post max attemps
    private static final int MAX_POST_ATTEMPS = 3;

    public static final long MAX_SIZE_BYTES = 5 * 1024 * 1024L;

    //Events memory queue
    private final BlockingQueue<Event> _eventQueue;

    //Centinel event used as checkpoint in FIFO queue
    static final private Event CENTINEL = new Event();

    //Track configuration
    private final HttpClient _httpclient;
    private final URI _eventsTarget;
    private final TrackClientConfig _config;
    private final PausableScheduledThreadPoolExecutor _flushScheduler;
    private final PausableScheduledThreadPoolExecutor _cachedflushScheduler;

    private final ExecutorService _senderExecutor;
    private final PausableThreadPoolExecutor _consumerExecutor;

    private final TrackStorageManager _storageManager;
    private static final String validationTag = "track";

    @VisibleForTesting
    public Consumer _consumer;

    // Estimated event size without properties
    public final static int EVENT_SIZE_WITHOUT_PROPS = 1024;

    private ThreadFactory eventClientThreadFactory(final String name) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Thread.currentThread().setPriority(MIN_PRIORITY);
                        r.run();
                    }
                }, name);
            }
        };
    }

    public static TrackClient create(TrackClientConfig config, HttpClient httpClient, URI eventsRootTarget, TrackStorageManager storageManager, ISplitCache splitCache) throws URISyntaxException {
        return new TrackClientImpl(config, new LinkedBlockingQueue<Event>(), httpClient, eventsRootTarget, storageManager, splitCache, null);
    }

    @VisibleForTesting
    public static TrackClient create(TrackClientConfig config, HttpClient httpClient, URI eventsRootTarget, TrackStorageManager storageManager, ISplitCache splitCache, ExecutorService senderExecutor) throws URISyntaxException {
        return new TrackClientImpl(config, new LinkedBlockingQueue<Event>(), httpClient, eventsRootTarget, storageManager, splitCache, senderExecutor);
    }

    private TrackClientImpl(TrackClientConfig config, BlockingQueue<Event> eventQueue, HttpClient httpclient, URI eventsRootTarget, TrackStorageManager storageManager, ISplitCache splitCache, ExecutorService senderExecutor) throws URISyntaxException {

        _storageManager = storageManager;

        _httpclient = httpclient;

        _eventsTarget = new URIBuilder(eventsRootTarget, "/events/bulk").build();

        _eventQueue = eventQueue;
        _config = config;

        if (senderExecutor == null) {
            // Thread to send events to backend
            _senderExecutor = new ThreadPoolExecutor(
                    1,
                    1,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(50),
                    eventClientThreadFactory("eventclient-sender"),
                    new RejectedExecutionHandler() {
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                            Logger.w("Executor queue full. Dropping events.");
                        }
                    });
        } else {
            _senderExecutor = senderExecutor;
        }

        // Queue consumer
        _consumer = new Consumer(_storageManager);
        _consumerExecutor = PausableThreadPoolExecutorImpl.newSingleThreadExecutor(eventClientThreadFactory("eventclient-consumer"));
        _consumerExecutor.submit(_consumer);


        // Events flusher
        _flushScheduler = PausableScheduledThreadPoolExecutorImpl.newSingleThreadScheduledExecutor(eventClientThreadFactory("eventclient-flush"));
        _flushScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                flush();
            }
        }, config.getFlushIntervalMillis(), config.getFlushIntervalMillis(), TimeUnit.SECONDS);

        // Cached events flusher
        _cachedflushScheduler = PausableScheduledThreadPoolExecutorImpl.newSingleThreadScheduledExecutor(eventClientThreadFactory("eventclient-cache-flush"));
        _cachedflushScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                flushFromLocalCache();
            }
        }, config.getFlushIntervalMillis(), config.getFlushIntervalMillis(), TimeUnit.SECONDS);

    }

    @Override
    public boolean track(Event event) {
        try {
            int sizeInBytes = EVENT_SIZE_WITHOUT_PROPS;
            if (event.properties != null) {

                if (event.properties.size() > 300) {
                    Logger.w(validationTag + "Event has more than 300 properties. Some of them will be trimmed when processed");
                }

                Map<String, Object> finalProperties = new HashMap<>(event.properties);
                Map<String, Object> properties = event.properties;
                for (Map.Entry entry : properties.entrySet()) {
                    if (entry.getValue() == null) {
                        continue;
                    }

                    String key = entry.getKey().toString();
                    Object value = entry.getValue();
                    if (!(value instanceof Number) &&
                            !(value instanceof Boolean) &&
                            !(value instanceof String)) {
                        finalProperties.put(entry.getKey().toString(), null);
                    }

                    sizeInBytes += (value.getClass() == String.class ? value.toString().getBytes().length : 0);
                    sizeInBytes += key.getBytes().length;

                    if (sizeInBytes > ValidationConfig.getInstance().getMaximumEventPropertyBytes()) {
                        Logger.w(validationTag + "The maximum size allowed for the properties is 32kb. Current is " + entry.getKey().toString() + ". Event not queued");
                        return false;
                    }
                }
                event.properties = finalProperties;
            }
            event.setSizeInBytes(sizeInBytes);
            _eventQueue.put(event);
        } catch (InterruptedException e) {
            Logger.w("Interruption when adding event withed while adding message %s.", event);
            return false;
        }
        return true;
    }

    @Override
    public void pause() {
        if (_consumerExecutor != null) {
            _consumerExecutor.pause();
        }

        if (_flushScheduler != null) {
            _flushScheduler.pause();
        }

        if (_cachedflushScheduler != null) {
            _cachedflushScheduler.pause();
        }
    }

    @Override
    public void resume() {
        if (_consumerExecutor != null) {
            _consumerExecutor.resume();
        }

        if (_flushScheduler != null) {
            _flushScheduler.resume();
        }

        if (_cachedflushScheduler != null) {
            _cachedflushScheduler.resume();
        }
    }

    @Override
    public void saveToDisk() {
        if (_consumer != null) {
            _consumer.saveToDisk();
        }
    }

    @Override
    public void close() {
        try {
            _consumerExecutor.shutdownNow();
            _flushScheduler.shutdownNow();
            _cachedflushScheduler.shutdownNow();
            _senderExecutor.awaitTermination(_config.hasToWaitBeforeShutdown(), TimeUnit.MILLISECONDS);
            _storageManager.close();
        } catch (Exception e) {
            Logger.w("Error when shutting down EventClientImpl", e);
        }
    }

    /**
     * the existence of this message in the queue triggers a send event in the consumer thread.
     */
    public void flush() {
        try {
            _eventQueue.put(CENTINEL);
        } catch (InterruptedException e) {
            Logger.w("Interruption when flusing events");
        }
    }

    private void flushFromLocalCache() {
        if (Utils.isSplitServiceReachable(_eventsTarget)) {

            List<EventsChunk> eventsChunks = _storageManager.takeAll();
            for (EventsChunk chunk : eventsChunks) {
                if (chunk.getAttempt() < MAX_POST_ATTEMPS) {
                    _senderExecutor.submit(EventSenderTask.create(_httpclient, _eventsTarget, chunk, _storageManager, _config.getMaxSentAttempts()));
                }
            }

        } else {
            Logger.i("Split events server cannot be reached out. Prevent post cached events");
        }
    }

    /**
     * Infinite loop that listens to event from the event queue, dequeue them and send them over once:
     * - a CENTINEL message has arrived, or
     * - the queue reached a specific size
     */
    class Consumer implements Runnable {

        private final TrackStorageManager _storageManager;
        List<Event> events;
        Consumer(TrackStorageManager storageManager) {
            _storageManager = storageManager;
            events = newEventList();
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void run() {

            long totalSizeInBytes = 0;
            try {
                while (true) {
                    Event event = _eventQueue.take();

                    if (event != CENTINEL) {
                        events.add(event);
                    } else if (events.size() < 1) {
                        Logger.d("No messages to publish.");
                        continue;
                    }

                    totalSizeInBytes += event.getSizeInBytes();
                    if (events.size() >= _config.getMaxQueueSize() || totalSizeInBytes >= MAX_SIZE_BYTES || event == CENTINEL) {
                        Logger.d(String.format("Sending %d events", events.size()));
                        if (events.size() > _config.getMaxEventsPerPost()) {
                            List<List<Event>> eventsChunks = Lists.partition(events, _config.getMaxEventsPerPost());
                            for (List<Event> eventsChunk : eventsChunks) {
                                // Dispatch
                                _senderExecutor.submit(EventSenderTask.create(_httpclient, _eventsTarget, new EventsChunk(eventsChunk), _storageManager, _config.getMaxSentAttempts()));
                            }
                        } else {
                            // Dispatch
                            _senderExecutor.submit(EventSenderTask.create(_httpclient, _eventsTarget, new EventsChunk(events), _storageManager, _config.getMaxSentAttempts()));
                        }
                        // Clear the queue of events for the next batch.
                        events = newEventList();
                    }
                }
            } catch (InterruptedException e) {
                Logger.w("Consumer thread was interrupted. Exiting...");
                //Saving event due to consumer interruption
                _storageManager.saveEvents(new EventsChunk(events));
            }
        }

        private List<Event> newEventList() {
            return Collections.synchronizedList(new ArrayList<>());
        }

        synchronized public void saveToDisk(){
            _storageManager.saveEvents(new EventsChunk(events));
            events = newEventList();
            _storageManager.saveToDisk();
        }


    }

    static class EventSenderTask implements Runnable {

        private final EventsChunk mChunk;
        private final URI mEndpoint;
        private final HttpClient mHttpClient;
        private final TrackStorageManager mTrackStorageManager;
        private final int mMaxSentAttempts;

        static EventSenderTask create(HttpClient httpclient, URI eventsTarget,
                                      EventsChunk events, TrackStorageManager storage, int maxSentAttempts) {
            return new EventSenderTask(httpclient, eventsTarget, events, storage, maxSentAttempts);
        }

        EventSenderTask(HttpClient httpClient, URI eventsTarget,
                        EventsChunk events, TrackStorageManager storage, int maxSentAttempts) {
            mHttpClient = httpClient;
            mChunk = events;
            mEndpoint = eventsTarget;
            mTrackStorageManager = storage;
            mMaxSentAttempts = maxSentAttempts;
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            boolean shouldSaveEvents = false;
            if (Utils.isSplitServiceReachable(mEndpoint)) {
                HttpResponse response;
                try {

                    String jsonEvents = (mChunk != null ? Json.toJson(mChunk.getEvents()) : null);
                    response = mHttpClient.request(mEndpoint, HttpMethod.POST, jsonEvents).execute();
                    if (!response.isSuccess()) {
                        Logger.d(String.format("Error posting events [error code: %d]", response.getHttpStatus()));
                        Logger.d("Caching events to next iteration");

                        //Saving events to disk
                        mChunk.addAtempt();
                        if (mChunk.getAttempt() < mMaxSentAttempts) {
                            shouldSaveEvents = true;
                        }
                    }
                } catch (HttpException e) {
                    shouldSaveEvents = true;
                    Logger.e("Error while sending track events: " + e.getLocalizedMessage());
                }
            } else {
                shouldSaveEvents = true;
            }
            if(shouldSaveEvents) {
                mTrackStorageManager.saveEvents(mChunk);
            }
        }
    }
}
