package io.split.android.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.split.android.client.dtos.Event;
import io.split.android.client.track.TrackStorageManager;
import io.split.android.client.utils.GenericClientUtil;
import io.split.android.client.utils.Logger;

import static java.lang.Thread.MIN_PRIORITY;


public class TrackClientImpl implements TrackClient {

    //Events memory queue
    private final BlockingQueue<Event> _eventQueue;

    //Centinel event used as checkpoint in FIFO queue
    static final Event CENTINEL = new Event();

    //TODO add a storage cache

    //Track configuration
    private final CloseableHttpClient _httpclient;
    private final URI _eventsTarget;
    private final int _waitBeforeShutdown;
    private final int _maxQueueSize;
    private final long _flushIntervalMillis;
    private final ScheduledExecutorService _flushScheduler;

    private final ExecutorService _senderExecutor;
    private final ExecutorService _consumerExecutor;

    private final TrackStorageManager _storageManager;

    ThreadFactory eventClientThreadFactory(final String name) {
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

    public static TrackClient create(CloseableHttpClient httpclient, URI eventsRootTarget, int maxQueueSize, long flushIntervalMillis, int waitBeforeShutdown, TrackStorageManager storageManager) throws URISyntaxException {
        return new TrackClientImpl(new LinkedBlockingQueue<Event>(), httpclient, eventsRootTarget, maxQueueSize, flushIntervalMillis, waitBeforeShutdown, storageManager);
    }

    public TrackClientImpl(BlockingQueue<Event> eventQueue, CloseableHttpClient httpclient, URI eventsRootTarget, int maxQueueSize,
                           long flushIntervalMillis, int waitBeforeShutdown, TrackStorageManager storageManager) throws URISyntaxException {


        _storageManager = storageManager;

        _httpclient = httpclient;

        _eventsTarget = new URIBuilder(eventsRootTarget).setPath("/api/events/bulk").build();

        _eventQueue = eventQueue;
        _waitBeforeShutdown = waitBeforeShutdown;

        _maxQueueSize = maxQueueSize;
        _flushIntervalMillis = flushIntervalMillis;

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

        // Queue consumer
        _consumerExecutor = Executors.newSingleThreadExecutor(eventClientThreadFactory("eventclient-consumer"));
        _consumerExecutor.submit(new Consumer(_storageManager));

        // Events flusher
        _flushScheduler = Executors.newScheduledThreadPool(1, eventClientThreadFactory("eventclient-flush"));
        _flushScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                flush();
            }
        }, _flushIntervalMillis, _flushIntervalMillis, TimeUnit.SECONDS);


    }


    @Override
    public boolean track(Event event) {
        try {
            if (event == null) {
                return false;
            }
            _eventQueue.put(event);
        } catch (InterruptedException e) {
            Logger.w("Interruption when adding event withed while adding message %s.", event);
            return false;
        }
        return true;
    }

    @Override
    public void close() {
        try {
            _consumerExecutor.shutdownNow();
            _flushScheduler.shutdownNow();
            _senderExecutor.awaitTermination(_waitBeforeShutdown, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Logger.w("Error when shutting down EventClientImpl", e);
        }
    }

    /**
     * the existence of this message in the queue triggers a send event in the consumer thread.
     */
    public void flush() {
        track(CENTINEL);
    }


    /**
     * Infinite loop that listens to event from the event queue, dequeue them and send them over once:
     *  - a CENTINEL message has arrived, or
     *  - the queue reached a specific size
     *
     */
    class Consumer implements Runnable {

        private final TrackStorageManager _storageManager;

        public Consumer(TrackStorageManager storageManager) {
            _storageManager = storageManager;
        }

        @Override
        public void run() {
            List<Event> events = new ArrayList<>();

            try {
                while (true) {
                    Event event = _eventQueue.take();

                    if (event != CENTINEL) {
                        events.add(event);
                    } else if (events.size() < 1) {
                        Logger.d("No messages to publish.");
                        continue;
                    }

                    if (events.size() >= _maxQueueSize || event == CENTINEL) {

                        Logger.d(String.format("Sending %d events", events.size()));

                        // Dispatch
                        _senderExecutor.submit(EventSenderTask.create(_httpclient, _eventsTarget, events, _storageManager));

                        // Clear the queue of events for the next batch.
                        events = new ArrayList<>();
                    }
                }
            } catch (InterruptedException e) {
                Logger.w("Consumer thread was interrupted. Exiting...");
                //Saving event due to consumer interruption
                _storageManager.saveEvents(events);
            }
        }
    }

    static class EventSenderTask implements Runnable {

        private final List<Event> _data;
        private final URI _endpoint;
        private final CloseableHttpClient _client;
        private final TrackStorageManager _storage;

        static EventSenderTask create(CloseableHttpClient httpclient, URI eventsTarget,
                                      List<Event> events, TrackStorageManager storage) {
            return new EventSenderTask(httpclient, eventsTarget, events, storage);
        }

        EventSenderTask(CloseableHttpClient httpclient, URI eventsTarget,
                        List<Event> events, TrackStorageManager storage) {
            _client = httpclient;
            _data = events;
            _endpoint = eventsTarget;
            _storage = storage;
        }

        @Override
        public void run() {

            int status = GenericClientUtil.POST(_data, _endpoint, _client);

            if (!(status >= 200 && status < 300)) {
                Logger.d(String.format("Error posting events [error code: %d]", status));
                Logger.d("Caching events to next iteration");

                _storage.saveEvents(_data);
            }

        }
    }

}
