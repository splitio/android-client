package io.split.android.client;

import io.split.android.client.api.Key;
import io.split.android.client.dtos.Event;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.utils.Logger;
import io.split.android.client.validators.KeyValidatorImpl;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.client.validators.TreatmentManager;
import io.split.android.client.validators.TreatmentManagerImpl;
import io.split.android.engine.experiments.SplitFetcher;
import io.split.android.engine.metrics.Metrics;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A basic implementation of SplitClient.
 *
 */
public final class SplitClientImpl implements SplitClient {

    private final SplitFactory mSplitFactory;
    private final SplitFetcher mSplitFetcher;
    private final ImpressionListener mImpressionListener;
    private final Metrics mMetrics;
    private final SplitClientConfig mConfig;
    private final String mMatchingKey;
    private final String mBucketingKey;
    private final SplitEventsManager mEventsManager;
    private final TrackClient mTrackClient;
    private final TreatmentManager mTreatmentManager;
    private static final double TRACK_DEFAULT_VALUE = 0.0;

    private boolean mIsClientDestroyed = false;

    public SplitClientImpl(SplitFactory container, Key key, SplitFetcher splitFetcher, ImpressionListener impressionListener, Metrics metrics, SplitClientConfig config, SplitEventsManager eventsManager, TrackClient trackClient) {
        mSplitFactory = container;
        mSplitFetcher = splitFetcher;
        mImpressionListener = impressionListener;
        mMetrics = metrics;
        mConfig = config;
        mMatchingKey = key.matchingKey();
        mBucketingKey = key.bucketingKey();
        mEventsManager = eventsManager;
        mTrackClient = trackClient;
        mTreatmentManager = new TreatmentManagerImpl(
                mMatchingKey, mBucketingKey, new EvaluatorImpl(mSplitFetcher),
                new KeyValidatorImpl(), new SplitValidatorImpl(splitFetcher), mMetrics,
                impressionListener, mConfig, eventsManager);

        checkNotNull(mSplitFetcher);
        checkNotNull(mImpressionListener);
        checkNotNull(mMatchingKey);
        checkNotNull(mEventsManager);
        checkNotNull(mTrackClient);

    }

    @Override
    public void destroy() {
        mIsClientDestroyed = true;
        mSplitFactory.destroy();
    }

    @Override
    public void flush() {
        mSplitFactory.flush();
    }

    @Override
    public boolean isReady() {
        return mSplitFactory.isReady();
    }

    @Override
    public String getTreatment(String split) {
        return getTreatment(split, Collections.<String, Object>emptyMap());
    }

    @Override
    public String getTreatment(String split, Map<String, Object> attributes) {
        return mTreatmentManager.getTreatment(split, attributes, mIsClientDestroyed);
    }

    @Override
    public SplitResult getTreatmentWithConfig(String split, Map<String, Object> attributes) {
        return mTreatmentManager.getTreatmentWithConfig(split, attributes, mIsClientDestroyed);
    }

    @Override
    public Map<String, String> getTreatments(List<String> splits, Map<String, Object> attributes) {
        return mTreatmentManager.getTreatments(splits, attributes, mIsClientDestroyed);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> splits, Map<String, Object> attributes) {
        return mTreatmentManager.getTreatmentsWithConfig(splits, attributes, mIsClientDestroyed);
    }

    public void on(SplitEvent event, SplitEventTask task){
        checkNotNull(event);
        checkNotNull(task);

        if(mEventsManager.eventAlreadyTriggered(event)) {
            Logger.w(String.format("A listener was added for %s on the SDK, which has already fired and won’t be emitted again. The callback won’t be executed.", event.toString()));
            return;
        }

        mEventsManager.register(event, task);
    }

    @Override
    public boolean track(String trafficType, String eventType) {
        return track(mMatchingKey, trafficType, eventType, TRACK_DEFAULT_VALUE, null);
    }

    @Override
    public boolean track(String trafficType, String eventType, double value) {
        return track(mMatchingKey, trafficType, eventType, value, null);
    }

    @Override
    public boolean track(String eventType) {
        return track(mMatchingKey, mConfig.trafficType(), eventType, TRACK_DEFAULT_VALUE, null);
    }

    @Override
    public boolean track(String eventType, double value) {
        return track(mMatchingKey, mConfig.trafficType(), eventType, value, null);
    }

    @Override
    public boolean track(String trafficType, String eventType, Map<String, Object> properties) {
        return track(mMatchingKey, trafficType, eventType, TRACK_DEFAULT_VALUE, properties);
    }

    @Override
    public boolean track(String trafficType, String eventType, double value, Map<String, Object> properties) {
        return track(mMatchingKey, trafficType, eventType, value, properties);
    }

    @Override
    public boolean track(String eventType, Map<String, Object> properties) {
        return track(mMatchingKey, mConfig.trafficType(), eventType, TRACK_DEFAULT_VALUE, properties);
    }

    @Override
    public boolean track(String eventType, double value, Map<String, Object> properties) {
        return track(mMatchingKey, mConfig.trafficType(), eventType, value, properties);
    }

    private boolean track(String key, String trafficType, String eventType, double value, Map<String, Object> properties) {

        if(mIsClientDestroyed) {
            Logger.e("Client has already been destroyed - no calls possible");
            return false;
        }

        Event event = new Event();
        event.eventTypeId = eventType;
        event.trafficTypeName = trafficType;
        event.key = key;
        event.value = value;
        event.timestamp = System.currentTimeMillis();
        event.properties = properties;


        return mTrackClient.track(event);
    }

}