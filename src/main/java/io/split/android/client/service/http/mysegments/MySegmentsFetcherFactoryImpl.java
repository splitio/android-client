package io.split.android.client.service.http.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.net.URI;
import java.net.URISyntaxException;

import io.split.android.client.network.HttpClient;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherImpl;
import io.split.android.client.service.http.HttpResponseParser;
import io.split.android.client.service.mysegments.SegmentResponseV2;
import io.split.android.client.utils.logger.Logger;

public class MySegmentsFetcherFactoryImpl implements MySegmentsFetcherFactory {

    private final String mEndpoint;
    private final HttpClient mHttpClient;
    private final HttpResponseParser<? extends SegmentResponseV2> mMySegmentsResponseParser;
    private final UriBuilder mUriBuilder;

    public MySegmentsFetcherFactoryImpl(@NonNull HttpClient httpClient,
                                        @NonNull String endpoint,
                                        @NonNull HttpResponseParser<? extends SegmentResponseV2> responseParser,
                                        @NonNull UriBuilder uriBuilder) {
        mHttpClient = checkNotNull(httpClient);
        mEndpoint = checkNotNull(endpoint);
        mMySegmentsResponseParser = checkNotNull(responseParser);
        mUriBuilder = uriBuilder;
    }

    @Override
    public HttpFetcher<? extends SegmentResponseV2> getFetcher(String matchingKey) {
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
