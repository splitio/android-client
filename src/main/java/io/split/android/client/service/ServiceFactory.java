package io.split.android.client.service;

import androidx.annotation.RestrictTo;

import java.net.URISyntaxException;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.SegmentResponse;
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
import io.split.android.client.service.impressions.unique.MTK;
import io.split.android.client.service.impressions.unique.MTKRequestBodySerializer;
import io.split.android.client.service.mysegments.MySegmentsResponseParser;
import io.split.android.client.service.splits.SplitChangeResponseParser;
import io.split.android.client.service.sseauthentication.SseAuthenticationResponseParser;
import io.split.android.client.telemetry.TelemetryConfigBodySerializer;
import io.split.android.client.telemetry.TelemetryStatsBodySerializer;
import io.split.android.client.telemetry.model.Config;
import io.split.android.client.telemetry.model.Stats;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ServiceFactory {

    public static HttpFetcher<SplitChange> getSplitsFetcher(
            HttpClient httpClient,
            String endPoint,
            String splitFilterQueryString) throws URISyntaxException {

        return new HttpFetcherImpl<>(httpClient,
                SdkTargetPath.splitChanges(endPoint, splitFilterQueryString),
                new SplitChangeResponseParser());
    }

    public static HttpFetcher<? extends SegmentResponse> getMySegmentsFetcher(
            HttpClient httpClient,
            String endPoint,
            String key) throws URISyntaxException {

        return new HttpFetcherImpl<>(httpClient,
                SdkTargetPath.mySegments(endPoint, key),
                new MySegmentsResponseParser());
    }

    public static HttpRecorder<List<Event>> getEventsRecorder(
            HttpClient httpClient,
            String endPoint) throws URISyntaxException {

        return new HttpRecorderImpl<>(
                httpClient, SdkTargetPath.events(endPoint),
                new EventsRequestBodySerializer());
    }

    public static HttpRecorder<List<KeyImpression>> getImpressionsRecorder(
            HttpClient httpClient,
            String endPoint) throws URISyntaxException {

        return new HttpRecorderImpl<>(
                httpClient, SdkTargetPath.impressions(endPoint),
                new ImpressionsRequestBodySerializer());
    }

    public static HttpRecorder<ImpressionsCount> getImpressionsCountRecorder(
            HttpClient httpClient,
            String endPoint) throws URISyntaxException {

        return new HttpRecorderImpl<>(
                httpClient, SdkTargetPath.impressionsCount(endPoint),
                new ImpressionsCountRequestBodySerializer());
    }

    public static HttpRecorder<MTK> getUniqueKeysRecorder(
            HttpClient httpClient,
            String endpoint) throws URISyntaxException {

        return new HttpRecorderImpl<>(
                httpClient, SdkTargetPath.uniqueKeys(endpoint),
                new MTKRequestBodySerializer());
    }

    public static HttpSseAuthTokenFetcher getSseAuthenticationFetcher(
            HttpClient httpClient,
            String endPoint) throws URISyntaxException {

        return new HttpSseAuthTokenFetcher(httpClient,
                SdkTargetPath.sseAuthentication(endPoint),
                new SseAuthenticationResponseParser());
    }

    public static HttpRecorder<Config> getTelemetryConfigRecorder(
            HttpClient httpClient,
            String endpoint) throws URISyntaxException {

        return new HttpRecorderImpl<>(
                httpClient, SdkTargetPath.telemetryConfig(endpoint),
                new TelemetryConfigBodySerializer());
    }

    public static HttpRecorder<Stats> getTelemetryStatsRecorder(
            HttpClient httpClient,
            String endpoint) throws URISyntaxException {

        return new HttpRecorderImpl<>(
                httpClient, SdkTargetPath.telemetryStats(endpoint),
                new TelemetryStatsBodySerializer()
        );
    }
}
