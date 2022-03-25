package io.split.android.client.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.SerializableEvent;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpRequest;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.HttpResponseImpl;
import io.split.android.client.network.SdkTargetPath;
import io.split.android.client.service.events.EventsRequestBodySerializer;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.service.http.HttpRecorderImpl;
import io.split.android.client.service.http.HttpRequestBodySerializer;
import io.split.android.client.service.impressions.ImpressionsRequestBodySerializer;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.NetworkHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpRecorderTest {

    private final static String TEST_URL = "http://testurl.com";
    private final static String EVENTS_TEST_URL = TEST_URL + SdkTargetPath.EVENTS;
    private final static String IMPRESSIONS_TEST_URL = TEST_URL + SdkTargetPath.IMPRESSIONS;

    NetworkHelper mNetworkHelperMock ;
    HttpClient mClientMock;
    URI mUrl;
    URI mEventsUrl;
    URI mImpressionsUrl;
    HttpRequestBodySerializer<List<Event>> mEventsRequestSerializer = new EventsRequestBodySerializer();

    @Before
    public void setup() throws URISyntaxException {
        mUrl = new URI(TEST_URL);
        mEventsUrl = new URI(EVENTS_TEST_URL);
        mImpressionsUrl = new URI(IMPRESSIONS_TEST_URL);
        mNetworkHelperMock = mock(NetworkHelper.class);
        mClientMock = mock(HttpClient.class);
    }

    @Test
    public void testNoReachableUrl() {

        when(mNetworkHelperMock.isReachable(mEventsUrl)).thenReturn(false);

        HttpRecorder<List<Event>> recorder = new HttpRecorderImpl<>(mClientMock, mUrl,
                mNetworkHelperMock, mEventsRequestSerializer);
        boolean isReachable = true;
        try {
            List<Event> events = new ArrayList<>();
            recorder.execute(events);
        } catch (HttpRecorderException e) {
            isReachable = false;
        }

        Assert.assertFalse(isReachable);
    }

    @Test
    public void testSuccessfulEventsSend() throws HttpException {
        boolean exceptionWasThrown = false;
        List<Event> events = createEvents();
        List<SerializableEvent> serializableEvents = createSerializedEventsObjects(events);
        String jsonEvents = Json.toJson(serializableEvents);
        when(mNetworkHelperMock.isReachable(mEventsUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = new HttpResponseImpl(200, "");
        when(request.execute()).thenReturn(response);
        when(mClientMock.request(mEventsUrl, HttpMethod.POST, jsonEvents)).thenReturn(request);

        HttpRecorder<List<Event>> recorder = new HttpRecorderImpl<>(mClientMock, mEventsUrl, mNetworkHelperMock, mEventsRequestSerializer);
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
    public void failedResponse() throws HttpException {

        when(mNetworkHelperMock.isReachable(mEventsUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        List<Event> events = createEvents();
        String jsonEvents = Json.toJson(createSerializedEventsObjects(events));

        HttpResponse response = new HttpResponseImpl(500, "");
        when(request.execute()).thenReturn(response);
        when(mClientMock.request(mEventsUrl, HttpMethod.POST, jsonEvents)).thenReturn(request);

        HttpRecorder<List<Event>> recorder = new HttpRecorderImpl<>(mClientMock, mEventsUrl, mNetworkHelperMock, mEventsRequestSerializer);
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


        HttpRecorder<List<Event>> recorder = new HttpRecorderImpl<>(mClientMock, mEventsUrl, mNetworkHelperMock, mEventsRequestSerializer);
        try {
            recorder.execute(events);
        } catch (HttpRecorderException e) {
            exceptionWasThrown = true;
        }

        Assert.assertTrue(exceptionWasThrown);
    }

    @Test
    public void successfulImpressionsSend() throws HttpException {
        boolean exceptionWasThrown = false;

        TestImpressions testImpression1 = new TestImpressions();
        TestImpressions testImpression2 = new TestImpressions();

        testImpression1.testName = "feature_1";
        testImpression1.keyImpressions = createImpressions(testImpression1.testName);

        testImpression2.testName = "feature_2";
        testImpression2.keyImpressions = createImpressions(testImpression2.testName);

        List<KeyImpression> impressions = new ArrayList(testImpression1.keyImpressions);
        impressions.addAll(testImpression2.keyImpressions);

        String jsonImpressions = Json.toJson(Arrays.asList(testImpression1, testImpression2));
        when(mNetworkHelperMock.isReachable(mImpressionsUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = new HttpResponseImpl(200, "");
        when(request.execute()).thenReturn(response);
        when(mClientMock.request(mImpressionsUrl, HttpMethod.POST, jsonImpressions)).thenReturn(request);
        ImpressionsRequestBodySerializer parser = (ImpressionsRequestBodySerializer) Mockito.mock(ImpressionsRequestBodySerializer.class);
        when(parser.serialize(impressions)).thenReturn(jsonImpressions);

        HttpRecorder<List<KeyImpression>> recorder = new HttpRecorderImpl<>(mClientMock, mImpressionsUrl, mNetworkHelperMock, parser);

        try {
            recorder.execute(impressions);
        } catch (HttpRecorderException e) {
            exceptionWasThrown = true;
        }

        Assert.assertFalse(exceptionWasThrown);
        verify(mClientMock, times(1)).request(mImpressionsUrl, HttpMethod.POST, jsonImpressions);
        verify(request, times(1)).execute();

    }

    private List<KeyImpression> createImpressions(String feature) {
        List<KeyImpression> impressions = new ArrayList<>();
        for(int i = 0; i <= 5; i++) {
            KeyImpression impression = new KeyImpression();
            impression.keyName = "Impression_" + i;
            impression.feature = feature;
            impression.time = 11111;
            impression.changeNumber = 9999L;
            impression.label  = "default rule";
            impressions.add(impression);
        }
        return impressions;
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

    private List<SerializableEvent> createSerializedEventsObjects(List<Event> events) {
        List<SerializableEvent> serializableEvents = new ArrayList<>();

        for (Event event : events) {
            SerializableEvent serializableEvent = new SerializableEvent();
            serializableEvent.eventTypeId = event.eventTypeId;
            serializableEvent.trafficTypeName = event.trafficTypeName;
            serializableEvent.key = event.key;
            serializableEvent.value = event.value;
            serializableEvent.timestamp = event.timestamp;
            serializableEvent.properties = event.properties;

            serializableEvents.add(serializableEvent);
        }

        return serializableEvents;
    }
}
