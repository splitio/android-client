package io.split.android.client.validators;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.EvaluationResult;
import io.split.android.client.Evaluator;
import io.split.android.client.FlagSetsFilter;
import io.split.android.client.SplitResult;
import io.split.android.client.TreatmentLabels;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.events.ListenableEventsManager;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.utils.logger.Logger;
import io.split.android.grammar.Treatments;

public class TreatmentManagerImpl implements TreatmentManager {

    private static class ValidationTag {
        public static final String GET_TREATMENT = "getTreatment";
        public static final String GET_TREATMENTS = "getTreatments";
        public static final String GET_TREATMENT_WITH_CONFIG = "getTreatmentWithConfig";
        public static final String GET_TREATMENTS_WITH_CONFIG = "getTreatmentsWithConfig";
        public static final String GET_TREATMENTS_BY_FLAG_SET = "getTreatmentsByFlagSet";
        public static final String GET_TREATMENTS_BY_FLAG_SETS = "getTreatmentsByFlagSets";
        public static final String GET_TREATMENTS_WITH_CONFIG_BY_FLAG_SET = "getTreatmentsWithConfigByFlagSet";
        public static final String GET_TREATMENTS_WITH_CONFIG_BY_FLAG_SETS = "getTreatmentsWithConfigByFlagSets";
    }

    private final Evaluator mEvaluator;
    private final KeyValidator mKeyValidator;
    private final SplitValidator mSplitValidator;
    private final ImpressionListener mImpressionListener;
    private final String mMatchingKey;
    private final String mBucketingKey;
    private final boolean mLabelsEnabled;
    private final ValidationMessageLogger mValidationLogger;
    private final ListenableEventsManager mEventsManager;
    @NonNull
    private final AttributesManager mAttributesManager;
    @NonNull
    private final AttributesMerger mAttributesMerger;
    private final TelemetryStorageProducer mTelemetryStorageProducer;
    private final FlagSetsFilter mFlagSetsFilter;
    private final SplitsStorage mSplitsStorage;
    private final SplitFilterValidator mFlagSetsValidator;

    public TreatmentManagerImpl(String matchingKey,
                                String bucketingKey,
                                Evaluator evaluator,
                                KeyValidator keyValidator,
                                SplitValidator splitValidator,
                                ImpressionListener impressionListener,
                                boolean labelsEnabled,
                                ListenableEventsManager eventsManager,
                                @NonNull AttributesManager attributesManager,
                                @NonNull AttributesMerger attributesMerger,
                                @NonNull TelemetryStorageProducer telemetryStorageProducer,
                                @Nullable FlagSetsFilter flagSetsFilter,
                                @NonNull SplitsStorage splitsStorage,
                                @NonNull ValidationMessageLogger validationLogger) {
        mEvaluator = evaluator;
        mKeyValidator = keyValidator;
        mSplitValidator = splitValidator;
        mMatchingKey = matchingKey;
        mBucketingKey = bucketingKey;
        mImpressionListener = impressionListener;
        mLabelsEnabled = labelsEnabled;
        mEventsManager = eventsManager;
        mValidationLogger = checkNotNull(validationLogger);
        mAttributesManager = checkNotNull(attributesManager);
        mAttributesMerger = checkNotNull(attributesMerger);
        mTelemetryStorageProducer = checkNotNull(telemetryStorageProducer);
        mFlagSetsFilter = flagSetsFilter;
        mSplitsStorage = checkNotNull(splitsStorage);
        mFlagSetsValidator = new FlagSetsValidatorImpl();
    }

    @Override
    public String getTreatment(String split, Map<String, Object> attributes, boolean isClientDestroyed) {
        try {
            return getTreatmentsWithConfigGeneric(
                    Collections.singletonList(split),
                    null,
                    attributes,
                    isClientDestroyed,
                    ValidationTag.GET_TREATMENT,
                    SplitResult::treatment,
                    Method.TREATMENT
            ).get(split);
        } catch (Exception ex) {
            // In case get fails for some reason
            Logger.e("Client " + ValidationTag.GET_TREATMENT + " exception", ex);
            mTelemetryStorageProducer.recordException(Method.TREATMENT);

            return Treatments.CONTROL;
        }
    }

    @Override
    public SplitResult getTreatmentWithConfig(String split, Map<String, Object> attributes, boolean isClientDestroyed) {
        try {
            return getTreatmentsWithConfigGeneric(
                    Collections.singletonList(split),
                    null,
                    attributes,
                    isClientDestroyed,
                    ValidationTag.GET_TREATMENT_WITH_CONFIG,
                    ResultTransformer::identity,
                    Method.TREATMENT_WITH_CONFIG
            ).get(split);
        } catch (Exception ex) {
            // In case get fails for some reason
            Logger.e("Client " + ValidationTag.GET_TREATMENT_WITH_CONFIG + " exception", ex);
            mTelemetryStorageProducer.recordException(Method.TREATMENT_WITH_CONFIG);

            return new SplitResult(Treatments.CONTROL);
        }
    }

    @Override
    public Map<String, String> getTreatments(List<String> splits, Map<String, Object> attributes, boolean isClientDestroyed) {
        return getTreatmentsWithConfigGeneric(
                splits,
                null,
                attributes,
                isClientDestroyed,
                ValidationTag.GET_TREATMENTS,
                SplitResult::treatment,
                Method.TREATMENTS);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> splits, Map<String, Object> attributes, boolean isClientDestroyed) {
        return getTreatmentsWithConfigGeneric(
                splits,
                null,
                attributes,
                isClientDestroyed,
                ValidationTag.GET_TREATMENTS_WITH_CONFIG,
                ResultTransformer::identity,
                Method.TREATMENTS_WITH_CONFIG);
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes, boolean isClientDestroyed) {
        return getTreatmentsWithConfigGeneric(
                null,
                Collections.singletonList(flagSet),
                attributes,
                isClientDestroyed,
                ValidationTag.GET_TREATMENTS_BY_FLAG_SET,
                SplitResult::treatment,
                Method.TREATMENTS_BY_FLAG_SET);
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes, boolean isClientDestroyed) {
        return getTreatmentsWithConfigGeneric(
                null,
                flagSets,
                attributes,
                isClientDestroyed,
                ValidationTag.GET_TREATMENTS_BY_FLAG_SETS,
                SplitResult::treatment,
                Method.TREATMENTS_BY_FLAG_SETS);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes, boolean isClientDestroyed) {
        return getTreatmentsWithConfigGeneric(
                null,
                Collections.singletonList(flagSet),
                attributes,
                isClientDestroyed,
                ValidationTag.GET_TREATMENTS_WITH_CONFIG_BY_FLAG_SET,
                ResultTransformer::identity,
                Method.TREATMENTS_WITH_CONFIG_BY_FLAG_SET);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes, boolean isClientDestroyed) {
        return getTreatmentsWithConfigGeneric(
                null,
                flagSets,
                attributes,
                isClientDestroyed,
                ValidationTag.GET_TREATMENTS_WITH_CONFIG_BY_FLAG_SETS,
                ResultTransformer::identity,
                Method.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS);
    }

    private <T> Map<String, T> getTreatmentsWithConfigGeneric(@Nullable List<String> names,
                                                              @Nullable List<String> flagSets,
                                                              @Nullable Map<String, Object> attributes,
                                                              boolean isClientDestroyed,
                                                              String validationTag,
                                                              ResultTransformer<T> resultTransformer,
                                                              Method telemetryMethodName) {
        try {
            // Check if client is destroyed. If so, return control treatments or empty map in the case of flag sets
            if (isClientDestroyed) {
                mValidationLogger.e("Client has already been destroyed - no calls possible", validationTag);

                return TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(
                        mSplitValidator,
                        mValidationLogger,
                        (names != null) ? names : new ArrayList<>(),
                        validationTag,
                        resultTransformer);
            }

            // If there are no names but we have flag sets, get the names from the flag sets
            if (names == null) {
                if (flagSets != null) {
                    names = getNamesFromSet(validationTag, new ArrayList<>(flagSets));
                } else {
                    names = new ArrayList<>();
                }
            }

            // Mark the start timestamp of the evaluation, to use for telemetry
            long start = System.currentTimeMillis();
            try {
                // Merge the stored attributes with the attributes passed in for this evaluation
                final Map<String, Object> mergedAttributes = mAttributesMerger.merge(mAttributesManager.getAllAttributes(), attributes);

                // Perform evaluations for every feature flag
                Map<String, T> result = new HashMap<>();
                for (String featureFlagName : names) {
                    SplitResult splitResult = getTreatmentWithConfigWithoutMetrics(featureFlagName, mergedAttributes, validationTag);
                    result.put(featureFlagName, resultTransformer.transform(splitResult));
                }

                return result;
            } finally {
                recordLatency(telemetryMethodName, start);
            }
        } catch (Exception exception) {
            Logger.e("Client " + validationTag + " exception", exception);
            mTelemetryStorageProducer.recordException(telemetryMethodName);

            return TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(
                    mSplitValidator,
                    mValidationLogger,
                    (names != null) ? names : new ArrayList<>(),
                    validationTag,
                    resultTransformer);
        }
    }

    private SplitResult getTreatmentWithConfigWithoutMetrics(String split, Map<String, Object> mergedAttributes, String validationTag) {
        // Validate Key
        ValidationErrorInfo errorInfo = mKeyValidator.validate(mMatchingKey, mBucketingKey);
        if (errorInfo != null) {
            mValidationLogger.e(errorInfo, validationTag);
            return new SplitResult(Treatments.CONTROL);
        }

        // Validate feature flag name
        String splitName = split;
        errorInfo = mSplitValidator.validateName(split);
        if (errorInfo != null) {
            if (errorInfo.isError()) {
                mValidationLogger.e(errorInfo, validationTag);
                return new SplitResult(Treatments.CONTROL);
            }
            mValidationLogger.w(errorInfo, validationTag);
            splitName = split.trim();
        }

        // Perform evaluation and create SplitResult object
        EvaluationResult evaluationResult = evaluateIfReady(splitName, mergedAttributes, validationTag);
        SplitResult splitResult = new SplitResult(evaluationResult.getTreatment(), evaluationResult.getConfigurations());

        // If the feature flag was not found, log the message and return the result
        if (evaluationResult.getLabel().equals(TreatmentLabels.DEFINITION_NOT_FOUND)) {
            mValidationLogger.w(mSplitValidator.splitNotFoundMessage(splitName), validationTag);
            return splitResult;
        }

        // Log impression
        logImpression(
                mMatchingKey,
                mBucketingKey,
                splitName,
                evaluationResult.getTreatment(),
                (mLabelsEnabled ? evaluationResult.getLabel() : null),
                evaluationResult.getChangeNumber(),
                mergedAttributes);

        return splitResult;
    }

    private void logImpression(String matchingKey, String bucketingKey, String splitName, String result, String label, Long changeNumber, Map<String, Object> attributes) {
        try {
            mImpressionListener.log(new Impression(matchingKey, bucketingKey, splitName, result, System.currentTimeMillis(), label, changeNumber, attributes));
        } catch (Throwable t) {
            Logger.e("An error occurred logging impression: " + t.getLocalizedMessage());
        }
    }

    private EvaluationResult evaluateIfReady(String featureFlagName,
                                             Map<String, Object> attributes, String validationTag) {
        if (!mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY) &&
                !mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY_FROM_CACHE)) {
            mValidationLogger.w("the SDK is not ready, results may be incorrect for feature flag " + featureFlagName + ". Make sure to wait for SDK readiness before using this method", validationTag);
            mTelemetryStorageProducer.recordNonReadyUsage();

            return new EvaluationResult(Treatments.CONTROL, TreatmentLabels.NOT_READY, null, null);
        }
        return mEvaluator.getTreatment(mMatchingKey, mBucketingKey, featureFlagName, attributes);
    }

    private void recordLatency(Method treatment, long startTime) {
        mTelemetryStorageProducer.recordLatency(treatment, System.currentTimeMillis() - startTime);
    }

    @NonNull
    private List<String> getNamesFromSet(@NonNull String method, @NonNull List<String> flagSets) {
        Set<String> setsToEvaluate = mFlagSetsValidator.items(method, flagSets, mFlagSetsFilter);

        if (setsToEvaluate.isEmpty()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(mSplitsStorage.getNamesByFlagSets(setsToEvaluate));
    }

    interface ResultTransformer<T> {

        T transform(SplitResult splitResult);

        static SplitResult identity(SplitResult splitResult) {
            return splitResult;
        }
    }
}
