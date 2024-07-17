package io.split.android.client.service.http.mysegments;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.service.http.HttpFetcher;

public interface MySegmentsFetcherFactory {

    HttpFetcher<List<MySegment>> getFetcher(String userKey);

    interface UriBuilder {

        URI build(String matchingKey) throws URISyntaxException;
    }
}
