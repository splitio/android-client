package io.split.android.client.service.http.mysegments;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import io.split.android.client.dtos.AllSegmentsChange;
import io.split.android.client.network.HttpClient;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpResponseParser;

public class MySegmentsFetcherFactoryImplTest {

    @Test
    public void getFetcherCallsUriBuilderImplementation() throws URISyntaxException {
        MySegmentsFetcherFactory.UriBuilder uriBuilder = mock(MySegmentsFetcherFactory.UriBuilder.class);
        when(uriBuilder.build("matchingKey")).thenReturn(mock(URI.class));
        HttpFetcher<AllSegmentsChange> fetcher = new MySegmentsFetcherFactoryImpl(mock(HttpClient.class),
                "endpoint",
                mock(HttpResponseParser.class),
                uriBuilder).getFetcher("matchingKey");

        verify(uriBuilder).build("matchingKey");
    }

    @Test
    public void getFetcherDoesNotCrashWhenUriBuilderFails() throws URISyntaxException {
        MySegmentsFetcherFactory.UriBuilder uriBuilder = mock(MySegmentsFetcherFactory.UriBuilder.class);
        when(uriBuilder.build("matchingKey")).thenThrow(new URISyntaxException("test", "test"));
        HttpFetcher<AllSegmentsChange> fetcher = new MySegmentsFetcherFactoryImpl(mock(HttpClient.class),
                "endpoint",
                mock(HttpResponseParser.class),
                uriBuilder).getFetcher("matchingKey");
    }
}
