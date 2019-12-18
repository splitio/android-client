package io.split.android.client.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpRequest;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.HttpResponseImpl;
import io.split.android.client.network.SdkTargetPath;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.NetworkHelper;
import io.split.android.engine.metrics.FetcherMetricsConfig;
import io.split.android.engine.metrics.Metrics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpRecorderTest {

    private final static String TEST_URL = "http://testurl.com";
    private final static String EVENTS_TEST_URL = TEST_URL + SdkTargetPath.EVENTS;

    NetworkHelper mNetworkHelperMock ;
    HttpClient mClientMock;
    URI mUrl;
    URI mEventsUrl;
    HttpRequestParser<List<Event>> mEventsRequestParser = new EventsRequestParser();

    @Before
    public void setup() throws URISyntaxException {
        mUrl = new URI(TEST_URL);
        mEventsUrl = new URI(EVENTS_TEST_URL);
        mNetworkHelperMock = mock(NetworkHelper.class);
        mClientMock = mock(HttpClient.class);
    }

    @Test
    public void testNoReachableUrl() {

        when(mNetworkHelperMock.isReachable(mEventsUrl)).thenReturn(false);

        HttpRecorder<List<Event>> recorder = new HttpRecorderImpl<>(mClientMock, mUrl,
                mNetworkHelperMock, mEventsRequestParser);
        boolean isReachable = true;
        try {
            List<Event> events = new ArrayList<>();
            recorder.execute(events);
        } catch (Exception e) {
            isReachable = false;
        }

        Assert.assertFalse(isReachable);
    }

    @Test
    public void testSuccessfulEventsSend() throws HttpException {
        boolean exceptionWasThrown = false;
        List<Event> events = createEvents();
        String jsonEvents = Json.toJson(events);
        when(mNetworkHelperMock.isReachable(mEventsUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = new HttpResponseImpl(200, "");
        when(request.execute()).thenReturn(response);
        when(mClientMock.request(mEventsUrl, HttpMethod.POST, jsonEvents)).thenReturn(request);

        HttpRecorder<List<Event>> recorder = new HttpRecorderImpl<>(mClientMock, mEventsUrl, mNetworkHelperMock, mEventsRequestParser);
        try {
            recorder.execute(events);
        } catch (HttpRecorderException e) {
            exceptionWasThrown = true;
        }

        Assert.assertFalse(exceptionWasThrown);
        verify(mClientMock, times(1)).request(mEventsUrl, HttpMethod.POST, jsonEvents);
        verify(request, times(1)).execute();

    }

    @Test
    public void failedResponse() throws URISyntaxException, HttpException {

        when(mNetworkHelperMock.isReachable(mEventsUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        List<Event> events = createEvents();
        String jsonEvents = Json.toJson(events);

        HttpResponse response = new HttpResponseImpl(500, "");
        when(request.execute()).thenReturn(response);
        when(mClientMock.request(mEventsUrl, HttpMethod.POST, jsonEvents)).thenReturn(request);

        HttpRecorder<List<Event>> recorder = new HttpRecorderImpl<>(mClientMock, mEventsUrl, mNetworkHelperMock, mEventsRequestParser);
        boolean exceptionWasThrown = false;
        try {
            recorder.execute(events);
        } catch (HttpRecorderException e) {
            exceptionWasThrown = true;
        }

        Assert.assertTrue(exceptionWasThrown);
        verify(mClientMock, times(1)).request(mEventsUrl, HttpMethod.POST, jsonEvents);
        verify(request, times(1)).execute();
    }

    @Test
    public void handleRequestException() throws URISyntaxException, HttpException {
        boolean exceptionWasThrown = false;
        List<Event> events = createEvents();
        String jsonEvents = Json.toJson(events);
        when(mNetworkHelperMock.isReachable(mEventsUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = new HttpResponseImpl(200, "wrong response here");
        when(request.execute()).thenThrow(RuntimeException.class);
        when(mClientMock.request(mEventsUrl, HttpMethod.POST, jsonEvents)).thenReturn(request);


        HttpRecorder<List<Event>> recorder = new HttpRecorderImpl<>(mClientMock, mEventsUrl, mNetworkHelperMock, mEventsRequestParser);
        try {
            recorder.execute(events);
        } catch (HttpRecorderException e) {
            exceptionWasThrown = true;
        }

        Assert.assertTrue(exceptionWasThrown);
    }

    private List<Event> createEvents() {
        List<Event> events = new ArrayList<>();
        for(int i = 0; i <= 5; i++) {
            Event event = new Event();
            event.eventTypeId = "event_" + i;
            event.trafficTypeName = "custom";
            event.key = "key1";
            events.add(event);
        }
        return events;
    }
}
