package io.split.android.client;

import com.google.common.collect.Lists;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
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
import io.split.android.client.track.EventsData;
import io.split.android.client.track.EventsDataList;
import io.split.android.client.track.EventsDataString;
import io.split.android.client.track.TrackStorageManager;
import io.split.android.client.utils.GenericClientUtil;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.Utils;

import static java.lang.Thread.MIN_PRIORITY;


public class TrackClientImpl implements TrackClient {

    //Events post max attemps
    private final int MAX_POST_ATTEMPS = 3;

    //Events memory queue
    private final BlockingQueue<Event> _eventQueue;

    //Centinel event used as checkpoint in FIFO queue
    static final Event CENTINEL = new Event();

    //Track configuration
    private final CloseableHttpClient _httpclient;
    private final URI _eventsTarget;
    private final int _waitBeforeShutdown;
    private final int _maxQueueSize;
    private final int _maxEventsPerPost;
    private final long _flushIntervalMillis;
    private final ScheduledExecutorService _flushScheduler;
    private final ScheduledExecutorService _cachedflushScheduler;

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

    public static TrackClient create(CloseableHttpClient httpclient, URI eventsRootTarget, int maxQueueSize, int maxEventsPerPost, long flushIntervalMillis, int waitBeforeShutdown, TrackStorageManager storageManager) throws URISyntaxException {
        return new TrackClientImpl(new LinkedBlockingQueue<Event>(), httpclient, eventsRootTarget, maxQueueSize, maxEventsPerPost, flushIntervalMillis, waitBeforeShutdown, storageManager);
    }

    public TrackClientImpl(BlockingQueue<Event> eventQueue, CloseableHttpClient httpclient, URI eventsRootTarget, int maxQueueSize, int maxEventsPerPost,
                           long flushIntervalMillis, int waitBeforeShutdown, TrackStorageManager storageManager) throws URISyntaxException {


        _storageManager = storageManager;

        _httpclient = httpclient;

        _eventsTarget = new URIBuilder(eventsRootTarget).setPath("/api/events/bulk").build();

        _eventQueue = eventQueue;
        _waitBeforeShutdown = waitBeforeShutdown;

        _maxQueueSize = maxQueueSize;
        _maxEventsPerPost = maxEventsPerPost;
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

        // Cached events flusher
        _cachedflushScheduler = Executors.newScheduledThreadPool(1, eventClientThreadFactory("eventclient-cache-flush"));
        _cachedflushScheduler.scheduleAtFixedRate(new Runnable() {
              @Override
              public void run() {flushFromLocalCache();}
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
            _cachedflushScheduler.shutdownNow();
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

    public void flushFromLocalCache(){

        if (Utils.isSplitServiceReachable(_eventsTarget)) {

            List<String> files = _storageManager.getAllChunkIds();

            for(String filename : files){
                String events = _storageManager.readCachedEvents(filename);
                int attemp = _storageManager.getLastAttemp(filename);

                if(attemp < MAX_POST_ATTEMPS) {
                    _senderExecutor.submit(EventSenderTask.create(_httpclient, _eventsTarget, EventsDataString.create(events), _storageManager, attemp + 1));
                }
                _storageManager.deleteCachedEvents(filename);
            }

        } else {
            Logger.i("Split events server cannot be reached out. Prevent post cached events");
        }
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

                        if(events.size() > _maxEventsPerPost){
                            List<List<Event>> chunks = Lists.partition(events, _maxEventsPerPost);
                            for (List<Event> chunk : chunks) {
                                // Dispatch
                                _senderExecutor.submit(EventSenderTask.create(_httpclient, _eventsTarget, EventsDataList.create(chunk), _storageManager, 0));
                            }
                        } else {
                            // Dispatch
                            _senderExecutor.submit(EventSenderTask.create(_httpclient, _eventsTarget, EventsDataList.create(events), _storageManager, 0));
                        }

                        // Clear the queue of events for the next batch.
                        events = new ArrayList<>();
                    }
                }
            } catch (InterruptedException e) {
                Logger.w("Consumer thread was interrupted. Exiting...");
                //Saving event due to consumer interruption
                _storageManager.saveEvents(events, 0);
            }
        }
    }

    static class EventSenderTask implements Runnable {

        private final int _attemp;
        private final EventsData _data;
        private final URI _endpoint;
        private final CloseableHttpClient _client;
        private final TrackStorageManager _storage;

        static EventSenderTask create(CloseableHttpClient httpclient, URI eventsTarget,
                                      EventsData events, TrackStorageManager storage, int attemp) {
            return new EventSenderTask(httpclient, eventsTarget, events, storage, attemp);
        }

        EventSenderTask(CloseableHttpClient httpclient, URI eventsTarget,
                        EventsData events, TrackStorageManager storage, int attemp) {
            _client = httpclient;
            _data = events;
            _endpoint = eventsTarget;
            _storage = storage;
            _attemp = attemp;
        }

        @Override
        public void run() {
            if (Utils.isSplitServiceReachable(_endpoint)) {
                int status = GenericClientUtil.POST(_data.asJSONEntity(), _endpoint, _client);

                if (!(status >= 200 && status < 300)) {
                    Logger.d(String.format("Error posting events [error code: %d]", status));
                    Logger.d("Caching events to next iteration");

                    //Saving events to disk
                    _storage.saveEvents(_data.toString(), _attemp);
                }
            } else {
                _storage.saveEvents(_data.toString(), _attemp);
            }
        }
    }

}
