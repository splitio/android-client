package io.split.android.client.service.http.mysegments;

import java.net.URI;
import java.net.URISyntaxException;

import io.split.android.client.dtos.AllSegmentsChange;
import io.split.android.client.service.http.HttpFetcher;

public interface MySegmentsFetcherFactory {

    HttpFetcher<AllSegmentsChange> getFetcher(String userKey);

    interface UriBuilder {

        URI build(String matchingKey) throws URISyntaxException;
    }
}
