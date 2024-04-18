package io.split.android.client.service.workmanager;

import java.net.URISyntaxException;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.network.HttpClient;
import io.split.android.client.service.ServiceFactory;
import io.split.android.client.service.http.HttpFetcher;

public class SplitsSyncWorkerFetcherProvider implements SplitsSyncWorkerTaskBuilder.FetcherProvider {

    private final HttpClient mHttpClient;
    private final String mEndpoint;

    SplitsSyncWorkerFetcherProvider(HttpClient httpClient, String endpoint) {
        mHttpClient = httpClient;
        mEndpoint = endpoint;
    }

    @Override
    public HttpFetcher<SplitChange> provideFetcher(String splitsFilterQueryString) throws URISyntaxException {
        return ServiceFactory.getSplitsFetcher(mHttpClient,
                mEndpoint, splitsFilterQueryString);
    }
}
