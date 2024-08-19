package io.split.android.client.service.http.mysegments;

import java.net.URI;
import java.net.URISyntaxException;

import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.mysegments.SegmentResponseV2;

public interface MySegmentsFetcherFactory {

    HttpFetcher<? extends SegmentResponseV2> getFetcher(String userKey);

    interface UriBuilder {

        URI build(String matchingKey) throws URISyntaxException;
    }
}
