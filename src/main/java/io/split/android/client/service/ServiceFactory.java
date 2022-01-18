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
import io.split.android.client.service.http.HttpSseAuthTokenFetcher;
import io.split.android.client.service.impressions.ImpressionsCount;
import io.split.android.client.service.impressions.ImpressionsCountRequestBodySerializer;
import io.split.android.client.service.impressions.ImpressionsRequestBodySerializer;
import io.split.android.client.service.mysegments.MySegmentsResponseParser;
import io.split.android.client.service.splits.SplitChangeResponseParser;
import io.split.android.client.service.sseauthentication.SseAuthenticationResponseParser;
import io.split.android.client.telemetry.TelemetryConfigBodySerializer;
import io.split.android.client.telemetry.TelemetryStatsBodySerializer;
import io.split.android.client.telemetry.model.Config;
import io.split.android.client.telemetry.model.Stats;
import io.split.android.client.utils.NetworkHelper;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ServiceFactory {

    public static HttpFetcher<SplitChange> getSplitsFetcher(
            NetworkHelper networkHelper,
            HttpClient httpClient,
            String endPoint,
            String splitFilterQueryString) throws URISyntaxException {

        return new HttpFetcherImpl<SplitChange>(httpClient,
                SdkTargetPath.splitChanges(endPoint, splitFilterQueryString),
                networkHelper, new SplitChangeResponseParser());
    }

    public static HttpFetcher<List<MySegment>> getMySegmentsFetcher(
            NetworkHelper networkHelper,
            HttpClient httpClient,
            String endPoint,
            String key) throws URISyntaxException {

        return new HttpFetcherImpl<List<MySegment>>(httpClient,
                SdkTargetPath.mySegments(endPoint, key),
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

    public static HttpRecorder<ImpressionsCount> getImpressionsCountRecorder(
            NetworkHelper networkHelper,
            HttpClient httpClient,
            String endPoint) throws URISyntaxException {
        return new HttpRecorderImpl<>(
                httpClient, SdkTargetPath.impressionsCount(endPoint), networkHelper,
                new ImpressionsCountRequestBodySerializer());
    }

    public static HttpSseAuthTokenFetcher getSseAuthenticationFetcher(
            NetworkHelper networkHelper,
            HttpClient httpClient,
            String endPoint) throws URISyntaxException {

        return new HttpSseAuthTokenFetcher(httpClient,
                SdkTargetPath.sseAuthentication(endPoint),
                networkHelper, new SseAuthenticationResponseParser());
    }

    public static HttpRecorder<Config> getTelemetryConfigRecorder(
            NetworkHelper networkHelper,
            HttpClient httpClient,
            String endpoint) throws URISyntaxException {
        return new HttpRecorderImpl<>(
                httpClient, SdkTargetPath.telemetryConfig(endpoint), networkHelper,
                new TelemetryConfigBodySerializer());
    }

    public static HttpRecorder<Stats> getTelemetryStatsRecorder(
            NetworkHelper networkHelper,
            HttpClient httpClient,
            String endpoint) throws URISyntaxException {
        return new HttpRecorderImpl<>(
                httpClient, SdkTargetPath.telemetryStats(endpoint), networkHelper,
                new TelemetryStatsBodySerializer()
        );
    }
}
