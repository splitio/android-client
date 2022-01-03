package io.split.android.client.service;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpSseAuthTokenFetcher;
import io.split.android.client.service.impressions.ImpressionsCount;
import io.split.android.client.service.impressions.ImpressionsCountPerFeature;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.telemetry.model.Config;
import io.split.android.client.telemetry.model.Stats;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitApiFacade {
    private final HttpFetcher<SplitChange> mSplitFetcher;
    private final HttpFetcher<List<MySegment>> mMySegmentsFetcher;
    private final HttpFetcher<SseAuthenticationResponse> mSseAuthenticationFetcher;
    private final HttpRecorder<List<Event>> mEventsRecorder;
    private final HttpRecorder<List<KeyImpression>> mImpressionsRecorder;
    private final HttpRecorder<ImpressionsCount> mImpressionsCountRecorder;
    private final HttpRecorder<Config> mTelemetryConfigRecorder;
    private final HttpRecorder<Stats> mTelemetryStatsRecorder;

    public SplitApiFacade(@NonNull HttpFetcher<SplitChange> splitFetcher,
                          @NonNull HttpFetcher<List<MySegment>> mySegmentsFetcher,
                          @NonNull HttpSseAuthTokenFetcher sseAuthenticationFetcher,
                          @NonNull HttpRecorder<List<Event>> eventsRecorder,
                          @NonNull HttpRecorder<List<KeyImpression>> impressionsRecorder,
                          @NonNull HttpRecorder<ImpressionsCount> impressionsCountRecorder,
                          @NonNull HttpRecorder<Config> telemetryConfigRecorder,
                          @NonNull HttpRecorder<Stats> telemetryStatsRecorder) {
        mSplitFetcher = checkNotNull(splitFetcher);
        mMySegmentsFetcher = checkNotNull(mySegmentsFetcher);
        mSseAuthenticationFetcher = checkNotNull(sseAuthenticationFetcher);
        mEventsRecorder = checkNotNull(eventsRecorder);
        mImpressionsRecorder = checkNotNull(impressionsRecorder);
        mImpressionsCountRecorder = checkNotNull(impressionsCountRecorder);
        mTelemetryConfigRecorder = checkNotNull(telemetryConfigRecorder);
        mTelemetryStatsRecorder = checkNotNull(telemetryStatsRecorder);
    }

    public HttpFetcher<SplitChange> getSplitFetcher() {
        return mSplitFetcher;
    }

    public HttpFetcher<List<MySegment>> getMySegmentsFetcher() {
        return mMySegmentsFetcher;
    }

    public HttpFetcher<SseAuthenticationResponse> getSseAuthenticationFetcher() {
        return mSseAuthenticationFetcher;
    }

    public HttpRecorder<List<Event>> getEventsRecorder() {
        return mEventsRecorder;
    }

    public HttpRecorder<List<KeyImpression>> getImpressionsRecorder() {
        return mImpressionsRecorder;
    }

    public HttpRecorder<ImpressionsCount> getImpressionsCountRecorder() {
        return mImpressionsCountRecorder;
    }

    public HttpRecorder<Config> getTelemetryConfigRecorder() {
        return mTelemetryConfigRecorder;
    }

    public HttpRecorder<Stats> getTelemetryStatsRecorder() {
        return mTelemetryStatsRecorder;
    }
}
