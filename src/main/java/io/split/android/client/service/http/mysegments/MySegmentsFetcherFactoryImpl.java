package io.split.android.client.service.http.mysegments;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.SdkTargetPath;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherImpl;
import io.split.android.client.service.mysegments.MySegmentsResponseParser;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.NetworkHelper;

public class MySegmentsFetcherFactoryImpl implements MySegmentsFetcherFactory {

    private final String mEndpoint;
    private final NetworkHelper mNetworkHelper;
    private final HttpClient mHttpClient;
    private final MySegmentsResponseParser mMySegmentsResponseParser;

    public MySegmentsFetcherFactoryImpl(@NonNull NetworkHelper networkHelper,
                                        @NonNull HttpClient httpClient,
                                        @NonNull String endpoint) {
        mNetworkHelper = checkNotNull(networkHelper);
        mHttpClient = checkNotNull(httpClient);
        mEndpoint = checkNotNull(endpoint);
        mMySegmentsResponseParser = new MySegmentsResponseParser();
    }

    @Override
    public HttpFetcher<List<MySegment>> getFetcher(String matchingKey) {
        return new HttpFetcherImpl<>(mHttpClient, buildTargetUrl(matchingKey), mNetworkHelper, mMySegmentsResponseParser);
    }

    private URI buildTargetUrl(String matchingKey) {
        try {
            return SdkTargetPath.mySegments(mEndpoint, matchingKey);
        } catch (URISyntaxException e) {
            Logger.e(e.getMessage());
        }

        return URI.create(mEndpoint);
    }
}
