package io.split.android.client.service.workmanager.splits;

import java.net.URISyntaxException;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.network.HttpClient;
import io.split.android.client.service.ServiceFactory;
import io.split.android.client.service.http.HttpFetcher;

class FetcherProvider {

    private final HttpClient mHttpClient;
    private final String mEndpoint;

    FetcherProvider(HttpClient httpClient, String endpoint) {
        mHttpClient = httpClient;
        mEndpoint = endpoint;
    }

    public HttpFetcher<SplitChange> provideFetcher(String splitsFilterQueryString) throws URISyntaxException {
        return ServiceFactory.getSplitsFetcher(mHttpClient,mEndpoint, splitsFilterQueryString);
    }
}
