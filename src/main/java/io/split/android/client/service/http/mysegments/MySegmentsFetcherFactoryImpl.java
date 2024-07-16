package io.split.android.client.service.http.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.network.HttpClient;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherImpl;
import io.split.android.client.service.http.HttpResponseParser;
import io.split.android.client.utils.logger.Logger;

public class MySegmentsFetcherFactoryImpl implements MySegmentsFetcherFactory {

    private final String mEndpoint;
    private final HttpClient mHttpClient;
    private final HttpResponseParser<List<MySegment>> mMySegmentsResponseParser;
    private final UriBuilder mUriBuilder;

    public MySegmentsFetcherFactoryImpl(@NonNull HttpClient httpClient,
                                        @NonNull String endpoint,
                                        @NonNull HttpResponseParser<List<MySegment>> responseParser,
                                        @NonNull UriBuilder uriBuilder) {
        mHttpClient = checkNotNull(httpClient);
        mEndpoint = checkNotNull(endpoint);
        mMySegmentsResponseParser = checkNotNull(responseParser);
        mUriBuilder = uriBuilder;
    }

    @Override
    public HttpFetcher<List<MySegment>> getFetcher(String matchingKey) {
        return new HttpFetcherImpl<>(mHttpClient, buildTargetUrl(matchingKey), mMySegmentsResponseParser);
    }

    private URI buildTargetUrl(String matchingKey) {
        try {
            return mUriBuilder.build(matchingKey);
        } catch (URISyntaxException e) {
            Logger.e(e.getMessage());
        }

        return URI.create(mEndpoint);
    }
}
