package io.split.android.client.service.mysegments;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.SdkTargetPath;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.service.BaseHttpFetcher;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.NetworkHelper;
import io.split.android.engine.metrics.Metrics;

import static com.google.common.base.Preconditions.checkNotNull;

public final class HttpMySegmentsFetcher extends BaseHttpFetcher implements MySegmentsFetcherV2 {

    static final private Type MY_SEGMENTS_RESPONSE_TYPE
            = new TypeToken<Map<String, List<MySegment>>>() {
    }.getType();

    final String mUserKey;

    public static HttpMySegmentsFetcher create(HttpClient client, URI root, Metrics metrics, NetworkHelper networkHelper, String userKey) throws URISyntaxException {
        return new HttpMySegmentsFetcher(client, new URIBuilder(root, SdkTargetPath.MY_SEGMENTS).build(), metrics, networkHelper, userKey);
    }

    public HttpMySegmentsFetcher(HttpClient client, URI uri, Metrics metrics, NetworkHelper networkHelper, String userKey) {
        super(client, uri, metrics, networkHelper);
        checkNotNull(userKey);
        mUserKey = userKey;
    }

    @Override
    public List<MySegment> execute() {

        long start = System.currentTimeMillis();
        List<MySegment> mySegments = null;
        try {
            checkIfSourceIsReachable();
            URI uri = new URIBuilder(mTarget, mUserKey).build();
            HttpResponse response = executeRequest(uri);
            checkResponseStatusAndThrowOnFail(response, Metrics.MY_SEGMENTS_FETCHER_STATUS);

            Map<String, List<MySegment>> mySegmentsMap = Json.fromJson(response.getData(), MY_SEGMENTS_RESPONSE_TYPE);
            mySegments = mySegmentsMap.get("mySegments");
            if (mySegments == null) {
                throwIlegalStatusException("Wrong data received from my segments server");
            }

        } catch (Exception e) {
            metricExceptionAndThrow(Metrics.MY_SEGMENTS_FETCHER_EXCEPTION, e);
        } finally {
            metricTime(Metrics.MY_SEGMENTS_FETCHER_TIME, System.currentTimeMillis() - start);
        }

        return mySegments;
    }
}
