package io.split.android.client;

import android.annotation.SuppressLint;

import com.google.common.collect.Lists;

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

import io.split.android.client.cache.ISplitCache;
import io.split.android.client.cache.ITrafficTypesCache;
import io.split.android.client.dtos.Event;
import io.split.android.client.track.EventsChunk;
import io.split.android.client.track.TrackClientConfig;
import io.split.android.client.track.TrackStorageManager;
import io.split.android.client.utils.GenericClientUtil;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.Utils;
import io.split.android.client.validators.EventValidator;
import io.split.android.client.validators.EventValidatorImpl;
import io.split.android.client.validators.KeyValidatorImpl;
import io.split.android.client.validators.ValidationErrorInfo;
import io.split.android.client.validators.ValidationMessageLogger;
import io.split.android.client.validators.ValidationMessageLoggerImpl;

import static java.lang.Thread.MIN_PRIORITY;


public class TrackClientImpl implements TrackClient {

    //Events post max attemps
    private static final int MAX_POST_ATTEMPS = 3;

    //Events memory queue
    private final BlockingQueue<Event> _eventQueue;

    //Centinel event used as checkpoint in FIFO queue
    static final private Event CENTINEL = new Event();

    //Track configuration
    private final CloseableHttpClient _httpclient;
    private final URI _eventsTarget;
    private final TrackClientConfig _config;
    private final ScheduledExecutorService _flushScheduler;
    private final ScheduledExecutorService _cachedflushScheduler;

    private final ExecutorService _senderExecutor;
    private final ExecutorService _consumerExecutor;

    private final TrackStorageManager _storageManager;
    private final String validationTag = "track";
    private final EventValidator _eventValidator;
    private final ValidationMessageLogger _validationLogger;

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

    public static TrackClient create(TrackClientConfig config, CloseableHttpClient httpclient, URI eventsRootTarget, TrackStorageManager storageManager, ISplitCache splitCache) throws URISyntaxException {
        return new TrackClientImpl(config, new LinkedBlockingQueue<Event>(), httpclient, eventsRootTarget, storageManager, splitCache);
    }

    private TrackClientImpl(TrackClientConfig config, BlockingQueue<Event> eventQueue, CloseableHttpClient httpclient, URI eventsRootTarget, TrackStorageManager storageManager, ISplitCache splitCache) throws URISyntaxException {


        _storageManager = storageManager;

        _httpclient = httpclient;

        _eventsTarget = new URIBuilder(eventsRootTarget).setPath("/api/events/bulk").build();

        _eventValidator = new EventValidatorImpl(new KeyValidatorImpl(), splitCache);

        _eventQueue = eventQueue;
        _config = config;

        _validationLogger = new ValidationMessageLoggerImpl();

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
        }, config.getFlushIntervalMillis(), config.getFlushIntervalMillis(), TimeUnit.SECONDS);

        // Cached events flusher
        _cachedflushScheduler = Executors.newScheduledThreadPool(1, eventClientThreadFactory("eventclient-cache-flush"));
        _cachedflushScheduler.scheduleAtFixedRate(new Runnable() {
              @Override
              public void run() {flushFromLocalCache();}
          }, config.getFlushIntervalMillis(), config.getFlushIntervalMillis(), TimeUnit.SECONDS);
    }

    @Override
    public boolean track(Event event) {
        try {

          if (event != CENTINEL) {
            ValidationErrorInfo errorInfo = _eventValidator.validate(event);
            if (errorInfo != null) {
                _validationLogger.log(errorInfo, validationTag);
                if(errorInfo.isError()) {
                    return false;
                }
                event.trafficTypeName = event.trafficTypeName.toLowerCase();
            }
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
        track(CENTINEL);
    }

    private void flushFromLocalCache(){

        if (Utils.isSplitServiceReachable(_eventsTarget)) {
            List<EventsChunk> eventsChunks = _storageManager.getEventsChunks();
            for(EventsChunk chunk : eventsChunks){
                if(chunk.getAttempt() < MAX_POST_ATTEMPS) {
                    _senderExecutor.submit(EventSenderTask.create(_httpclient, _eventsTarget, chunk, _storageManager, _config.getMaxSentAttempts()));
                }
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

        Consumer(TrackStorageManager storageManager) {
            _storageManager = storageManager;
        }

        @SuppressLint("DefaultLocale")
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

                    if (events.size() >= _config.getMaxQueueSize() || event == CENTINEL) {

                        Logger.d(String.format("Sending %d events", events.size()));

                        if(events.size() > _config.getMaxEventsPerPost()){
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
                        events = new ArrayList<>();
                    }
                }
            } catch (InterruptedException e) {
                Logger.w("Consumer thread was interrupted. Exiting...");
                //Saving event due to consumer interruption
                _storageManager.saveEvents(new EventsChunk(events));
            }
        }
    }

    static class EventSenderTask implements Runnable {

        private final EventsChunk mChunk;
        private final URI mEndpoint;
        private final CloseableHttpClient mHttpClient;
        private final TrackStorageManager mTrackStorageManager;
        private final int mMaxSentAttempts;

        static EventSenderTask create(CloseableHttpClient httpclient, URI eventsTarget,
                                      EventsChunk events, TrackStorageManager storage, int maxSentAttempts) {
            return new EventSenderTask(httpclient, eventsTarget, events, storage, maxSentAttempts);
        }

        EventSenderTask(CloseableHttpClient httpclient, URI eventsTarget,
                        EventsChunk events, TrackStorageManager storage, int maxSentAttempts) {
            mHttpClient = httpclient;
            mChunk = events;
            mEndpoint = eventsTarget;
            mTrackStorageManager = storage;
            mMaxSentAttempts = maxSentAttempts;
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            if (Utils.isSplitServiceReachable(mEndpoint)) {
                int status = GenericClientUtil.POST(mChunk.asJSONEntity(), mEndpoint, mHttpClient);

                if (!(status >= 200 && status < 300)) {
                    Logger.d(String.format("Error posting events [error code: %d]", status));
                    Logger.d("Caching events to next iteration");

                    //Saving events to disk
                    mChunk.addAtempt();
                    if(mChunk.getAttempt() <mMaxSentAttempts) {
                        mTrackStorageManager.saveEvents(mChunk);
                    } else {
                        mTrackStorageManager.deleteCachedEvents(mChunk.getId());
                    }
                }
            } else {
                mTrackStorageManager.saveEvents(mChunk);
            }
        }
    }

}
