package io.split.android.client.validators;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.Evaluator;
import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.ListenableEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;

public class TreatmentManagerFactoryImpl implements TreatmentManagerFactory {

    private final KeyValidator mKeyValidator;
    private final SplitValidator mSplitValidator;
    private final ImpressionListener mCustomerImpressionListener;
    private final boolean mLabelsEnabled;
    private final AttributesMerger mAttributesMerger;
    private final TelemetryStorageProducer mTelemetryStorageProducer;
    private final Evaluator mEvaluator;

    public TreatmentManagerFactoryImpl(@NonNull KeyValidator keyValidator,
                                       @NonNull SplitValidator splitValidator,
                                       @NonNull ImpressionListener customerImpressionListener,
                                       boolean labelsEnabled,
                                       @NonNull AttributesMerger attributesMerger,
                                       @NonNull TelemetryStorageProducer telemetryStorageProducer,
                                       @NonNull Evaluator evaluator) {
        mKeyValidator = checkNotNull(keyValidator);
        mSplitValidator = checkNotNull(splitValidator);
        mCustomerImpressionListener = checkNotNull(customerImpressionListener);
        mLabelsEnabled = labelsEnabled;
        mAttributesMerger = checkNotNull(attributesMerger);
        mTelemetryStorageProducer = checkNotNull(telemetryStorageProducer);
        mEvaluator = checkNotNull(evaluator);
    }

    @Override
    public TreatmentManager getTreatmentManager(Key key, ListenableEventsManager eventsManager, AttributesManager attributesManager) {
        return new TreatmentManagerImpl(
                key.matchingKey(),
                key.bucketingKey(),
                mEvaluator,
                mKeyValidator,
                mSplitValidator,
                mCustomerImpressionListener,
                mLabelsEnabled,
                eventsManager,
                attributesManager,
                mAttributesMerger,
                mTelemetryStorageProducer
        );
    }
}
