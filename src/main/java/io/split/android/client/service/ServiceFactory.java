package io.split.android.client.service;

import androidx.annotation.RestrictTo;

import java.net.URISyntaxException;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.SdkTargetPath;
import io.split.android.client.service.events.EventsRequestBodySerializer;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherImpl;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderImpl;
import io.split.android.client.service.impressions.ImpressionsRequestBodySerializer;
import io.split.android.client.service.mysegments.MySegmentsResponseParser;
import io.split.android.client.service.splits.SplitChangeResponseParser;
import io.split.android.client.service.sseauthentication.SseAuthenticationResponseParser;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.utils.NetworkHelper;
import io.split.android.engine.metrics.FetcherMetricsConfig;
import io.split.android.engine.metrics.Metrics;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ServiceFactory {

    public static HttpFetcher<SplitChange> getSplitsFetcher(
            NetworkHelper networkHelper,
            HttpClient httpClient,
            String endPoint,
            Metrics cachedFireAndForgetMetrics) throws URISyntaxException {

        FetcherMetricsConfig splitsfetcherMetricsConfig = new FetcherMetricsConfig(
                Metrics.SPLIT_CHANGES_FETCHER_EXCEPTION,
                Metrics.SPLIT_CHANGES_FETCHER_TIME,
                Metrics.SPLIT_CHANGES_FETCHER_STATUS
        );

        return new HttpFetcherImpl<SplitChange>(httpClient,
                SdkTargetPath.splitChanges(endPoint), cachedFireAndForgetMetrics,
                splitsfetcherMetricsConfig,
                networkHelper, new SplitChangeResponseParser());
    }

    public static HttpFetcher<List<MySegment>> getMySegmentsFetcher(
            NetworkHelper networkHelper,
            HttpClient httpClient,
            String endPoint,
            String key,
            Metrics cachedFireAndForgetMetrics) throws URISyntaxException {
        FetcherMetricsConfig mySegmentsfetcherMetricsConfig = new FetcherMetricsConfig(
                Metrics.MY_SEGMENTS_FETCHER_EXCEPTION,
                Metrics.MY_SEGMENTS_FETCHER_TIME,
                Metrics.MY_SEGMENTS_FETCHER_STATUS
        );

        return new HttpFetcherImpl<List<MySegment>>(httpClient,
                SdkTargetPath.mySegments(endPoint, key), cachedFireAndForgetMetrics,
                mySegmentsfetcherMetricsConfig,
                networkHelper, new MySegmentsResponseParser());
    }

    public static HttpRecorder<List<Event>> getEventsRecorder(
            NetworkHelper networkHelper,
            HttpClient httpClient,
            String endPoint) throws URISyntaxException {
        return new HttpRecorderImpl<>(
                httpClient, SdkTargetPath.events(endPoint), networkHelper,
                new EventsRequestBodySerializer());
    }

    public static HttpRecorder<List<KeyImpression>> getImpressionsRecorder(
            NetworkHelper networkHelper,
            HttpClient httpClient,
            String endPoint) throws URISyntaxException {
        return new HttpRecorderImpl<>(
                httpClient, SdkTargetPath.impressions(endPoint), networkHelper,
                new ImpressionsRequestBodySerializer());
    }

    public static HttpFetcher<SseAuthenticationResponse> getSseAuthenticationFetcher(
            NetworkHelper networkHelper,
            HttpClient httpClient,
            String endPoint) throws URISyntaxException {

        return new HttpFetcherImpl<SseAuthenticationResponse>(httpClient,
                SdkTargetPath.sseAuthentication(endPoint),
                networkHelper, new SseAuthenticationResponseParser());
    }
}
