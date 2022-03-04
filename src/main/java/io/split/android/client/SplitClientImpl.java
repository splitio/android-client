package io.split.android.client;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

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
import io.split.android.client.shared.SplitClientContainer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.utils.Logger;
import io.split.android.client.validators.EventValidator;
import io.split.android.client.validators.EventValidatorImpl;
import io.split.android.client.validators.KeyValidatorImpl;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.TreatmentManager;
import io.split.android.client.validators.TreatmentManagerHelper;
import io.split.android.client.validators.TreatmentManagerImpl;
import io.split.android.client.validators.ValidationErrorInfo;
import io.split.android.client.validators.ValidationMessageLogger;
import io.split.android.client.validators.ValidationMessageLoggerImpl;
import io.split.android.engine.experiments.SplitParser;
import io.split.android.grammar.Treatments;

public final class SplitClientImpl implements SplitClient {

    private final SplitFactory mSplitFactory;
    private final SplitClientContainer mClientContainer;
    private final SplitClientConfig mConfig;
    private final String mMatchingKey;
    private final SplitEventsManager mEventsManager;
    private final EventPropertiesProcessor mEventPropertiesProcessor;
    private final TreatmentManager mTreatmentManager;
    private final EventValidator mEventValidator;
    private final ValidationMessageLogger mValidationLogger;
    private final SyncManager mSyncManager;
    private final AttributesManager mAttributesManager;
    private final TelemetryStorageProducer mTelemetryStorageProducer;
    private final SplitValidator mSplitValidator;

    private static final double TRACK_DEFAULT_VALUE = 0.0;

    private boolean mIsClientDestroyed = false;

    public SplitClientImpl(SplitFactory container,
                           SplitClientContainer clientContainer,
                           Key key,
                           SplitParser splitParser,
                           ImpressionListener impressionListener,
                           SplitClientConfig config,
                           SplitEventsManager eventsManager,
                           SplitsStorage splitsStorage,
                           EventPropertiesProcessor eventPropertiesProcessor,
                           SyncManager syncManager,
                           AttributesManager attributesManager,
                           TelemetryStorageProducer telemetryStorageProducer,
                           SplitValidator splitValidator,
                           TreatmentManager treatmentManager) {
        this(container,
                clientContainer,
                key,
                splitParser,
                impressionListener,
                config,
                eventsManager,
                eventPropertiesProcessor,
                new EventValidatorImpl(new KeyValidatorImpl(), splitsStorage),
                syncManager,
                attributesManager,
                telemetryStorageProducer,
                treatmentManager,
                splitValidator);
    }

    @VisibleForTesting
    public SplitClientImpl(SplitFactory container,
                           SplitClientContainer clientContainer,
                           Key key,
                           SplitParser splitParser,
                           ImpressionListener impressionListener,
                           SplitClientConfig config,
                           SplitEventsManager eventsManager,
                           EventPropertiesProcessor eventPropertiesProcessor,
                           EventValidator eventValidator,
                           SyncManager syncManager,
                           AttributesManager attributesManager,
                           TelemetryStorageProducer telemetryStorageProducer,
                           TreatmentManager treatmentManager,
                           SplitValidator splitValidator) {
        checkNotNull(splitParser);
        checkNotNull(impressionListener);

        mSplitFactory = checkNotNull(container);
        mClientContainer = checkNotNull(clientContainer);
        mMatchingKey = checkNotNull(key.matchingKey());
        mConfig = checkNotNull(config);
        mEventsManager = checkNotNull(eventsManager);
        mEventValidator = checkNotNull(eventValidator);
        mValidationLogger = new ValidationMessageLoggerImpl();
        mTelemetryStorageProducer = telemetryStorageProducer;
        mTreatmentManager = treatmentManager;
        mEventPropertiesProcessor = checkNotNull(eventPropertiesProcessor);
        mSyncManager = checkNotNull(syncManager);
        mAttributesManager = checkNotNull(attributesManager);
        mSplitValidator = checkNotNull(splitValidator);
    }

    @Override
    public void destroy() {
        mIsClientDestroyed = true;
        mClientContainer.remove(mMatchingKey);
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
        try {
            return mTreatmentManager.getTreatment(split, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e("Client getTreatment exception", exception);

            mTelemetryStorageProducer.recordException(Method.TREATMENT);

            return Treatments.CONTROL;
        }
    }

    @Override
    public SplitResult getTreatmentWithConfig(String split, Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatmentWithConfig(split, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e("Client getTreatmentWithConfig exception", exception);

            mTelemetryStorageProducer.recordException(Method.TREATMENT_WITH_CONFIG);

            return new SplitResult(Treatments.CONTROL, TreatmentLabels.EXCEPTION);
        }
    }

    @Override
    public Map<String, String> getTreatments(List<String> splits, Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatments(splits, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e("Client getTreatments exception", exception);

            mTelemetryStorageProducer.recordException(Method.TREATMENTS);

            return TreatmentManagerHelper.controlTreatmentsForSplits(splits, mSplitValidator);
        }
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> splits, Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatmentsWithConfig(splits, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e("Client getTreatmentsWithConfig exception", exception);

            mTelemetryStorageProducer.recordException(Method.TREATMENTS_WITH_CONFIG);

            return TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(splits, mSplitValidator);
        }
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
        try {
            return mAttributesManager.setAttribute(attributeName, value);
        } catch (Exception exception) {
            Logger.e("Error setting attribute: " + exception.getLocalizedMessage());

            return false;
        }
    }

    @Nullable
    @Override
    public Object getAttribute(String attributeName) {
        try {
            return mAttributesManager.getAttribute(attributeName);
        } catch (Exception exception) {
            Logger.e("Error getting attribute: " + exception.getLocalizedMessage());

            return null;
        }
    }

    @Override
    public boolean setAttributes(Map<String, Object> attributes) {
        try {
            return mAttributesManager.setAttributes(attributes);
        } catch (Exception exception) {
            Logger.e("Error setting attributes: " + exception.getLocalizedMessage());

            return false;
        }
    }

    @NonNull
    @Override
    public Map<String, Object> getAllAttributes() {
        try {
            return mAttributesManager.getAllAttributes();
        } catch (Exception exception) {
            Logger.e("Error getting attributes: " + exception.getLocalizedMessage());

            return Collections.emptyMap();
        }
    }

    @Override
    public boolean removeAttribute(String attributeName) {
        try {
            return mAttributesManager.removeAttribute(attributeName);
        } catch (Exception exception) {
            Logger.e("Error removing attribute: " + exception.getLocalizedMessage());

            return false;
        }
    }

    @Override
    public boolean clearAttributes() {
        try {
            return mAttributesManager.clearAttributes();
        } catch (Exception exception) {
            Logger.e("Error clearing attributes: " + exception.getLocalizedMessage());

            return false;
        }
    }

    // Estimated event size without properties
    private final static int ESTIMATED_EVENT_SIZE_WITHOUT_PROPS = 1024;

    private boolean track(String key, String trafficType, String eventType, double value, Map<String, Object> properties) {
        try {
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

            long startTime = System.currentTimeMillis();

            event.properties = processedProperties.getProperties();
            event.setSizeInBytes(ESTIMATED_EVENT_SIZE_WITHOUT_PROPS + processedProperties.getSizeInBytes());
            mSyncManager.pushEvent(event);

            mTelemetryStorageProducer.recordLatency(Method.TRACK, System.currentTimeMillis() - startTime);

            return true;
        } catch (Exception exception) {
            mTelemetryStorageProducer.recordException(Method.TRACK);

            return false;
        }
    }
}
