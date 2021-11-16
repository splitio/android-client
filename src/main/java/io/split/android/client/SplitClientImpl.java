package io.split.android.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMergerImpl;
import io.split.android.client.dtos.Event;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.storage.splits.SplitsStorage;
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
import io.split.android.engine.experiments.SplitParser;
import io.split.android.engine.metrics.Metrics;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A basic implementation of SplitClient.
 */
public final class SplitClientImpl implements SplitClient {

    private final SplitFactory mSplitFactory;
    private final SplitClientConfig mConfig;
    private final String mMatchingKey;
    private final SplitEventsManager mEventsManager;
    private final EventPropertiesProcessor mEventPropertiesProcessor;
    private final TreatmentManager mTreatmentManager;
    private final EventValidator mEventValidator;
    private final ValidationMessageLogger mValidationLogger;
    private final SyncManager mSyncManager;
    private final AttributesManager mAttributesManager;

    private static final double TRACK_DEFAULT_VALUE = 0.0;

    private boolean mIsClientDestroyed = false;

    public SplitClientImpl(SplitFactory container,
                           Key key,
                           SplitParser splitParser,
                           ImpressionListener impressionListener,
                           Metrics metrics,
                           SplitClientConfig config,
                           SplitEventsManager eventsManager,
                           SplitsStorage splitsStorage,
                           EventPropertiesProcessor eventPropertiesProcessor,
                           SyncManager syncManager,
                           AttributesManager attributesManager) {

        checkNotNull(splitParser);
        checkNotNull(impressionListener);

        String mBucketingKey = key.bucketingKey();
        mMatchingKey = checkNotNull(key.matchingKey());

        mSplitFactory = checkNotNull(container);
        mConfig = checkNotNull(config);
        mEventsManager = checkNotNull(eventsManager);
        mEventValidator = new EventValidatorImpl(new KeyValidatorImpl(), splitsStorage);
        mValidationLogger = new ValidationMessageLoggerImpl();
        mTreatmentManager = new TreatmentManagerImpl(
                mMatchingKey, mBucketingKey, new EvaluatorImpl(splitsStorage, splitParser),
                new KeyValidatorImpl(), new SplitValidatorImpl(), metrics,
                impressionListener, mConfig, eventsManager, attributesManager, new AttributesMergerImpl());
        mEventPropertiesProcessor = checkNotNull(eventPropertiesProcessor);
        mSyncManager = checkNotNull(syncManager);
        mAttributesManager = checkNotNull(attributesManager);
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
        return mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY);
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

    public void on(SplitEvent event, SplitEventTask task) {
        checkNotNull(event);
        checkNotNull(task);

        if (!event.equals(SplitEvent.SDK_READY_FROM_CACHE) && mEventsManager.eventAlreadyTriggered(event)) {
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

    @Override
    public boolean setAttribute(String attributeName, Object value) {
        return mAttributesManager.setAttribute(attributeName, value);
    }

    @Nullable
    @Override
    public Object getAttribute(String attributeName) {
        return mAttributesManager.getAttribute(attributeName);
    }

    @Override
    public boolean setAttributes(Map<String, Object> attributes) {
        return mAttributesManager.setAttributes(attributes);
    }

    @NonNull
    @Override
    public Map<String, Object> getAllAttributes() {
        return mAttributesManager.getAllAttributes();
    }

    @Override
    public boolean removeAttribute(String attributeName) {
        return mAttributesManager.removeAttribute(attributeName);
    }

    @Override
    public boolean clearAttributes() {
        return mAttributesManager.clearAttributes();
    }

    // Estimated event size without properties
    private final static int ESTIMATED_EVENT_SIZE_WITHOUT_PROPS = 1024;

    private boolean track(String key, String trafficType, String eventType, double value, Map<String, Object> properties) {
        final String validationTag = "track";
        final boolean isSdkReady = mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY);
        if (mIsClientDestroyed) {
            mValidationLogger.e("Client has already been destroyed - no calls possible", validationTag);
            return false;
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

            if (errorInfo.isError()) {
                mValidationLogger.e(errorInfo, validationTag);
                return false;
            }
            mValidationLogger.w(errorInfo, validationTag);
            event.trafficTypeName = event.trafficTypeName.toLowerCase();
        }

        ProcessedEventProperties processedProperties =
                mEventPropertiesProcessor.process(event.properties);
        if (!processedProperties.isValid()) {
            return false;
        }
        event.properties = processedProperties.getProperties();
        event.setSizeInBytes(ESTIMATED_EVENT_SIZE_WITHOUT_PROPS + processedProperties.getSizeInBytes());
        mSyncManager.pushEvent(event);
        return true;
    }
}
