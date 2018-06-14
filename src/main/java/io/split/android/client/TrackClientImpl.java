package io.split.android.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import io.split.android.client.dtos.Event;
import io.split.android.client.utils.Logger;


public class TrackClientImpl implements TrackClient, Runnable {

    //Events memory queue
    private final BlockingQueue<Event> _eventQueue;

    //TODO add a storage cache

    //Track configuration
    private final CloseableHttpClient _httpclient;
    private final URI _eventsTarget;
    private final int _waitBeforeShutdown;
    private final int _maxQueueSize;
    private final long _flushIntervalMillis;
    private final ScheduledExecutorService _scheduler;

    public static TrackClient create(CloseableHttpClient httpclient, URI eventsRootTarget, int maxQueueSize, long flushIntervalMillis, int waitBeforeShutdown) throws URISyntaxException {
        return new TrackClientImpl(new LinkedBlockingQueue<Event>(), httpclient, eventsRootTarget, maxQueueSize, flushIntervalMillis, waitBeforeShutdown);
    }

    public TrackClientImpl(BlockingQueue<Event> eventQueue, CloseableHttpClient httpclient, URI eventsRootTarget, int maxQueueSize,
                           long flushIntervalMillis, int waitBeforeShutdown) throws URISyntaxException {


        _httpclient = httpclient;

        _eventsTarget = new URIBuilder(eventsRootTarget).setPath("/api/events/bulk").build();

        _eventQueue = eventQueue;
        _waitBeforeShutdown = waitBeforeShutdown;

        _maxQueueSize = maxQueueSize;
        _flushIntervalMillis = flushIntervalMillis;

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-ImpressionsManager-%d")
                .build();
        _scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
        _scheduler.scheduleAtFixedRate(this, 10, _flushIntervalMillis, TimeUnit.SECONDS);


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

    }

    @Override
    public void run() {
        flushEvents();
    }

    private void flushEvents(){
        Logger.i("*** Flushing events!!!!!");
        Logger.d(_eventQueue.toString());
    }
}
