package io.split.android.client;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.dtos.Event;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.validators.EventValidator;
import io.split.android.client.validators.ValidationErrorInfo;
import io.split.android.client.validators.ValidationMessageLogger;

public class EventsTrackerImpl implements EventsTracker {
    // Estimated event size without properties
    private final static int ESTIMATED_EVENT_SIZE_WITHOUT_PROPS = 1024;

    private final EventValidator mEventValidator;
    private final ValidationMessageLogger mValidationLogger;
    private final TelemetryStorageProducer mTelemetryStorageProducer;
    private final EventPropertiesProcessor mEventPropertiesProcessor;
    private final SyncManager mSyncManager;
    private final AtomicBoolean isTrackingEnabled = new AtomicBoolean(true);

    public EventsTrackerImpl(@NonNull EventValidator eventValidator,
                             @NonNull ValidationMessageLogger validationLogger,
                             @NonNull TelemetryStorageProducer telemetryStorageProducer,
                             @NonNull EventPropertiesProcessor eventPropertiesProcessor,
                             @NonNull SyncManager syncManager) {

        mEventValidator = checkNotNull(eventValidator);
        mValidationLogger = checkNotNull(validationLogger);
        mTelemetryStorageProducer = checkNotNull(telemetryStorageProducer);
        mEventPropertiesProcessor = checkNotNull(eventPropertiesProcessor);
        mSyncManager = checkNotNull(syncManager);
    }

    public void enableTracking(boolean enable) {
        isTrackingEnabled.set(enable);
    }
    public boolean track(String key, String trafficType, String eventType,
                         double value, Map<String, Object> properties, boolean isSdkReady) {

        if (!isTrackingEnabled.get()) {
            Logger.v("Event not tracked because tracking is disabled");
            return false;
        }

        try {
            final String validationTag = "track";

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
        }
        return false;
    }
}
