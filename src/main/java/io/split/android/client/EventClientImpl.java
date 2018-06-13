package io.split.android.client;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import io.split.android.client.dtos.Event;
import io.split.android.client.utils.Logger;


public class EventClientImpl implements EventClient {

    //Events memory queue
    private final BlockingQueue<Event> _eventQueue;

    //TODO add a storage cache

    //Track configuration
    private final CloseableHttpClient _httpclient;
    private final URI _eventsTarget;
    private final int _waitBeforeShutdown;
    private final int _maxQueueSize;
    private final long _flushIntervalMillis;

    public static EventClient create(CloseableHttpClient httpclient, URI eventsRootTarget, int maxQueueSize, long flushIntervalMillis, int waitBeforeShutdown) throws URISyntaxException {
        return new EventClientImpl(new LinkedBlockingQueue<Event>(), httpclient, eventsRootTarget, maxQueueSize, flushIntervalMillis, waitBeforeShutdown);
    }

    public EventClientImpl(BlockingQueue<Event> eventQueue, CloseableHttpClient httpclient, URI eventsRootTarget, int maxQueueSize,
                           long flushIntervalMillis, int waitBeforeShutdown) throws URISyntaxException {


        _httpclient = httpclient;

        _eventsTarget = new URIBuilder(eventsRootTarget).setPath("/api/events/bulk").build();

        _eventQueue = eventQueue;
        _waitBeforeShutdown = waitBeforeShutdown;

        _maxQueueSize = maxQueueSize;
        _flushIntervalMillis = flushIntervalMillis;

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
}
