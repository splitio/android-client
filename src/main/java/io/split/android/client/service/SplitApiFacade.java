package io.split.android.client.service;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.mysegments.MySegmentsFetcherFactory;
import io.split.android.client.service.impressions.ImpressionsCount;
import io.split.android.client.service.impressions.unique.MTK;
import io.split.android.client.service.mysegments.MembershipsResponse;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.telemetry.model.Config;
import io.split.android.client.telemetry.model.Stats;

public class SplitApiFacade {
    private final HttpFetcher<SplitChange> mSplitFetcher;
    private final MySegmentsFetcherFactory mMySegmentsFetcherFactory;
    private final HttpFetcher<SseAuthenticationResponse> mSseAuthenticationFetcher;
    private final HttpRecorder<List<Event>> mEventsRecorder;
    private final HttpRecorder<List<KeyImpression>> mImpressionsRecorder;
    private final HttpRecorder<ImpressionsCount> mImpressionsCountRecorder;
    private final HttpRecorder<MTK> mUniqueKeysRecorder;
    private final HttpRecorder<Config> mTelemetryConfigRecorder;
    private final HttpRecorder<Stats> mTelemetryStatsRecorder;

    public SplitApiFacade(@NonNull HttpFetcher<SplitChange> splitFetcher,
                          @NonNull MySegmentsFetcherFactory mySegmentsFetcherFactory,
                          @NonNull HttpFetcher<SseAuthenticationResponse> sseAuthenticationFetcher,
                          @NonNull HttpRecorder<List<Event>> eventsRecorder,
                          @NonNull HttpRecorder<List<KeyImpression>> impressionsRecorder,
                          @NonNull HttpRecorder<ImpressionsCount> impressionsCountRecorder,
                          @NonNull HttpRecorder<MTK> uniqueKeysRecorder,
                          @NonNull HttpRecorder<Config> telemetryConfigRecorder,
                          @NonNull HttpRecorder<Stats> telemetryStatsRecorder) {
        mSplitFetcher = checkNotNull(splitFetcher);
        mMySegmentsFetcherFactory = checkNotNull(mySegmentsFetcherFactory);
        mSseAuthenticationFetcher = checkNotNull(sseAuthenticationFetcher);
        mEventsRecorder = checkNotNull(eventsRecorder);
        mImpressionsRecorder = checkNotNull(impressionsRecorder);
        mImpressionsCountRecorder = checkNotNull(impressionsCountRecorder);
        mUniqueKeysRecorder = checkNotNull(uniqueKeysRecorder);
        mTelemetryConfigRecorder = checkNotNull(telemetryConfigRecorder);
        mTelemetryStatsRecorder = checkNotNull(telemetryStatsRecorder);
    }

    public HttpFetcher<SplitChange> getSplitFetcher() {
        return mSplitFetcher;
    }

    public HttpFetcher<? extends MembershipsResponse> getMySegmentsFetcher(String matchingKey) {
        return mMySegmentsFetcherFactory.getFetcher(matchingKey);
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

    public HttpRecorder<MTK> getUniqueKeysRecorder() {
        return mUniqueKeysRecorder;
    }

    public HttpRecorder<Config> getTelemetryConfigRecorder() {
        return mTelemetryConfigRecorder;
    }

    public HttpRecorder<Stats> getTelemetryStatsRecorder() {
        return mTelemetryStatsRecorder;
    }
}
