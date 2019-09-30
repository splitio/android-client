package io.split.android.client;

import io.split.android.client.api.Key;
import io.split.android.client.cache.ISplitCache;
import io.split.android.client.dtos.Event;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.utils.Logger;
import io.split.android.client.validators.EventValidator;
import io.split.android.client.validators.EventValidatorImpl;
import io.split.android.client.validators.KeyValidatorImpl;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.client.validators.TreatmentManager;
import io.split.android.client.validators.TreatmentManagerImpl;
import io.split.android.client.validators.ValidationErrorInfo;
import io.split.android.client.validators.ValidationMessageLogger;
import io.split.android.client.validators.ValidationMessageLoggerImpl;
import io.split.android.engine.experiments.SplitFetcher;
import io.split.android.engine.metrics.Metrics;


import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A basic implementation of SplitClient.
 *
 */
public final class SplitClientImpl implements SplitClient {

    private final SplitFactory mSplitFactory;
    private final SplitClientConfig mConfig;
    private final String mMatchingKey;
    private final SplitEventsManager mEventsManager;
    private final TrackClient mTrackClient;
    private final TreatmentManager mTreatmentManager;
    private final EventValidator mEventValidator;
    private final ValidationMessageLogger mValidationLogger;

    private static final double TRACK_DEFAULT_VALUE = 0.0;

    private boolean mIsClientDestroyed = false;

    public SplitClientImpl(SplitFactory container,
                           Key key,
                           SplitFetcher splitFetcher,
                           ImpressionListener impressionListener,
                           Metrics metrics,
                           SplitClientConfig config,
                           SplitEventsManager eventsManager,
                           TrackClient trackClient,
                           ISplitCache splitCache) {

        String mBucketingKey = key.bucketingKey();
        mMatchingKey = key.matchingKey();

        mSplitFactory = container;
        mConfig = config;
        mEventsManager = eventsManager;
        mTrackClient = trackClient;
        mEventValidator = new EventValidatorImpl(new KeyValidatorImpl(), splitCache);
        mValidationLogger = new ValidationMessageLoggerImpl();
        mTreatmentManager = new TreatmentManagerImpl(
                mMatchingKey, mBucketingKey, new EvaluatorImpl(splitFetcher),
                new KeyValidatorImpl(), new SplitValidatorImpl(splitFetcher), metrics,
                impressionListener, mConfig, eventsManager);

        checkNotNull(splitFetcher);
        checkNotNull(impressionListener);
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
        return getTreatment(split, Collections.emptyMap());
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
        final String validationTag = "track";
        final boolean isSdkReady = mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY);
        if(mIsClientDestroyed) {
            mValidationLogger.e("Client has already been destroyed - no calls possible", validationTag);
            return false;
        }

        if(!isSdkReady) {
            mValidationLogger.w("the SDK is not ready, results may be incorrect. Make sure to wait for SDK readiness before using this method", validationTag);
        }

        Event event = new Event();
        event.eventTypeId = eventType;
        event.trafficTypeName = trafficType;
        event.key = key;
        event.value = value;
        event.timestamp = System.currentTimeMillis();
        event.properties = properties;

        ValidationErrorInfo errorInfo = mEventValidator.validate(event, isSdkReady);
        if (errorInfo != null) {

            if(errorInfo.isError()) {
                mValidationLogger.e(errorInfo, validationTag);
                return false;
            }
            mValidationLogger.w(errorInfo, validationTag);
            event.trafficTypeName = event.trafficTypeName.toLowerCase();
        }

        return mTrackClient.track(event);
    }

}