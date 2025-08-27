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

import io.split.android.client.EvaluationOptions;
import io.split.android.client.EvaluationResult;
import io.split.android.client.Evaluator;
import io.split.android.client.FlagSetsFilter;
import io.split.android.client.SplitResult;
import io.split.android.client.TreatmentLabels;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.events.ListenableEventsManager;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.fallback.FallbackTreatment;
import io.split.android.client.fallback.FallbackTreatmentsCalculator;
import io.split.android.client.impressions.DecoratedImpression;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class TreatmentManagerImpl implements TreatmentManager {

    private final Evaluator mEvaluator;
    private final KeyValidator mKeyValidator;
    private final SplitValidator mSplitValidator;
    private final ImpressionListener.FederatedImpressionListener mImpressionListener;
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
    private final PropertyValidator mPropertyValidator;
    @NonNull
    private final FallbackTreatmentsCalculator mFallbackCalculator;

    public TreatmentManagerImpl(String matchingKey,
                                String bucketingKey,
                                Evaluator evaluator,
                                KeyValidator keyValidator,
                                SplitValidator splitValidator,
                                ImpressionListener.FederatedImpressionListener impressionListener,
                                boolean labelsEnabled,
                                ListenableEventsManager eventsManager,
                                @NonNull AttributesManager attributesManager,
                                @NonNull AttributesMerger attributesMerger,
                                @NonNull TelemetryStorageProducer telemetryStorageProducer,
                                @Nullable FlagSetsFilter flagSetsFilter,
                                @NonNull SplitsStorage splitsStorage,
                                @NonNull ValidationMessageLogger validationLogger,
                                @NonNull SplitFilterValidator flagSetsValidator,
                                @NonNull PropertyValidator propertyValidator,
                                @NonNull FallbackTreatmentsCalculator fallbackCalculator) {
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
        mFlagSetsValidator = checkNotNull(flagSetsValidator);
        mPropertyValidator = checkNotNull(propertyValidator);
        mFallbackCalculator = checkNotNull(fallbackCalculator);
    }

    @Override
    public String getTreatment(String split, Map<String, Object> attributes, EvaluationOptions evaluationOptions, boolean isClientDestroyed) {
        try {
            String treatment = getTreatmentsWithConfigGeneric(
                    Collections.singletonList(split),
                    null,
                    attributes,
                    evaluationOptions,
                    isClientDestroyed,
                    SplitResult::treatment,
                    Method.TREATMENT
            ).get(split);

            if (treatment == null) {
                FallbackTreatment fallback = mFallbackCalculator.resolve(split);
                return fallback.getTreatment();
            }
            return treatment;
        } catch (Exception ex) {
            // In case get fails for some reason
            Logger.e("Client " + Method.TREATMENT.getMethod() + " exception", ex);

            mTelemetryStorageProducer.recordException(Method.TREATMENT);

            FallbackTreatment fallback = mFallbackCalculator.resolve(split);
            return fallback.getTreatment();
        }
    }

    @Override
    public SplitResult getTreatmentWithConfig(String split, Map<String, Object> attributes, EvaluationOptions evaluationOptions, boolean isClientDestroyed) {
        try {
            SplitResult splitResult = getTreatmentsWithConfigGeneric(
                    Collections.singletonList(split),
                    null,
                    attributes,
                    evaluationOptions,
                    isClientDestroyed,
                    ResultTransformer::identity,
                    Method.TREATMENT_WITH_CONFIG
            ).get(split);

            if (splitResult == null) {
                FallbackTreatment fallback = mFallbackCalculator.resolve(split);
                return new SplitResult(fallback.getTreatment(), fallback.getConfig());
            }
            return splitResult;
        } catch (Exception ex) {
            // In case get fails for some reason
            Logger.e("Client " + Method.TREATMENT_WITH_CONFIG.getMethod() + " exception", ex);
            mTelemetryStorageProducer.recordException(Method.TREATMENT_WITH_CONFIG);

            FallbackTreatment fallback = mFallbackCalculator.resolve(split);
            return new SplitResult(fallback.getTreatment(), fallback.getConfig());
        }
    }

    @Override
    public Map<String, String> getTreatments(List<String> splits, Map<String, Object> attributes, EvaluationOptions evaluationOptions, boolean isClientDestroyed) {
        return getTreatmentsWithConfigGeneric(
                splits,
                null,
                attributes,
                evaluationOptions,
                isClientDestroyed,
                SplitResult::treatment,
                Method.TREATMENTS);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> splits, Map<String, Object> attributes, EvaluationOptions evaluationOptions, boolean isClientDestroyed) {
        return getTreatmentsWithConfigGeneric(
                splits,
                null,
                attributes,
                evaluationOptions,
                isClientDestroyed,
                ResultTransformer::identity,
                Method.TREATMENTS_WITH_CONFIG);
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes, EvaluationOptions evaluationOptions, boolean isClientDestroyed) {
        return getTreatmentsWithConfigGeneric(
                null,
                Collections.singletonList(flagSet),
                attributes,
                evaluationOptions,
                isClientDestroyed,
                SplitResult::treatment,
                Method.TREATMENTS_BY_FLAG_SET);
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes, EvaluationOptions evaluationOptions, boolean isClientDestroyed) {
        return getTreatmentsWithConfigGeneric(
                null,
                flagSets,
                attributes,
                evaluationOptions,
                isClientDestroyed,
                SplitResult::treatment,
                Method.TREATMENTS_BY_FLAG_SETS);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes, EvaluationOptions evaluationOptions, boolean isClientDestroyed) {
        return getTreatmentsWithConfigGeneric(
                null,
                Collections.singletonList(flagSet),
                attributes,
                evaluationOptions,
                isClientDestroyed,
                ResultTransformer::identity,
                Method.TREATMENTS_WITH_CONFIG_BY_FLAG_SET);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes, EvaluationOptions evaluationOptions, boolean isClientDestroyed) {
        return getTreatmentsWithConfigGeneric(
                null,
                flagSets,
                attributes,
                evaluationOptions,
                isClientDestroyed,
                ResultTransformer::identity,
                Method.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS);
    }

    private <T> Map<String, T> getTreatmentsWithConfigGeneric(@Nullable List<String> names,
                                                              @Nullable List<String> flagSets,
                                                              @Nullable Map<String, Object> attributes,
                                                              EvaluationOptions evaluationOptions,
                                                              boolean isClientDestroyed,
                                                              ResultTransformer<T> resultTransformer,
                                                              Method telemetryMethodName) {
        // This flag will be modified if there are any exceptions caught in getTreatmentWithConfigWithoutMetrics,
        // in which case an exception will be recorded in telemetry
        boolean exceptionsOccurred = false;
        String validationTag = telemetryMethodName.getMethod();
        try {
            // Check if client is destroyed. If so, return control treatments or empty map in the case of flag sets
            if (isClientDestroyed) {
                mValidationLogger.e("Client has already been destroyed - no calls possible", validationTag);

                return getControlTreatmentsForSplitsWithConfig(names, validationTag, resultTransformer);
            }

            // Validate Key
            ValidationErrorInfo errorInfo = mKeyValidator.validate(mMatchingKey, mBucketingKey);
            if (errorInfo != null) {
                mValidationLogger.e(errorInfo, validationTag);
                return getControlTreatmentsForSplitsWithConfig(names, validationTag, resultTransformer);
            }

            // If there are no names but we have flag sets, get the names from the flag sets
            if (names == null) {
                if (flagSets != null) {
                    names = getNamesFromSet(validationTag, flagSets);
                } else {
                    names = new ArrayList<>();
                }
            }

            // Mark the start timestamp of the evaluation, to use for telemetry
            long start = System.currentTimeMillis();
            try {
                // Merge the stored attributes with the attributes passed in for this evaluation
                final Map<String, Object> mergedAttributes = mAttributesMerger.merge(mAttributesManager.getAllAttributes(), attributes);

                // Create the result map
                Map<String, T> result = new HashMap<>();

                // Perform evaluations for every feature flag
                for (String featureFlagName : names) {
                    TreatmentResult evaluationResult = getTreatmentWithConfigWithoutMetrics(featureFlagName, mergedAttributes, validationTag, evaluationOptions);

                    result.put(featureFlagName, resultTransformer.transform(evaluationResult.getSplitResult()));
                    if (evaluationResult.isException()) {
                        exceptionsOccurred = true;
                    }
                }

                return result;
            } finally {
                recordLatency(telemetryMethodName, start);
                if (exceptionsOccurred) {
                    mTelemetryStorageProducer.recordException(telemetryMethodName);
                }
            }
        } catch (Exception exception) {
            Logger.e("Client " + validationTag + " exception", exception);
            mTelemetryStorageProducer.recordException(telemetryMethodName);

            return getControlTreatmentsForSplitsWithConfig(names, validationTag, resultTransformer);
        }
    }

    private TreatmentResult getTreatmentWithConfigWithoutMetrics(String split, Map<String, Object> mergedAttributes, String validationTag, EvaluationOptions evaluationOptions) {
        EvaluationResult evaluationResult = null;
        try {

            // Validate feature flag name
            String splitName = split;
            ValidationErrorInfo errorInfo = mSplitValidator.validateName(split);
            if (errorInfo != null) {
                if (errorInfo.isError()) {
                    mValidationLogger.e(errorInfo, validationTag);
                    FallbackTreatment fallback = mFallbackCalculator.resolve(split.trim());
                    return new TreatmentResult(new SplitResult(fallback.getTreatment(), fallback.getConfig()), false);
                }
                mValidationLogger.w(errorInfo, validationTag);
                splitName = split.trim();
            }

            // Perform evaluation and create SplitResult object
            evaluationResult = evaluateIfReady(splitName, mergedAttributes, validationTag);
            SplitResult splitResult = new SplitResult(evaluationResult.getTreatment(), evaluationResult.getConfigurations());

            // If the feature flag was not found, log the message and return the result
            if (evaluationResult.getLabel().equals(TreatmentLabels.DEFINITION_NOT_FOUND)) {
                mValidationLogger.w(mSplitValidator.splitNotFoundMessage(splitName), validationTag);
                return new TreatmentResult(splitResult, false);
            }

            // Log impression
            logImpression(
                    mMatchingKey,
                    mBucketingKey,
                    splitName,
                    evaluationResult.getTreatment(),
                    mLabelsEnabled ? evaluationResult.getLabel() : null,
                    evaluationResult.getChangeNumber(),
                    mergedAttributes,
                    evaluationResult.isImpressionsDisabled(),
                    evaluationOptions,
                    validationTag);

            return new TreatmentResult(splitResult, false);
        } catch (Exception ex) {
            // Since this only logs an impression with EXCEPTION label, we don't log anything if labels are disabled
            FallbackTreatment resolvedTreatment = mFallbackCalculator.resolve(split);
            if (mLabelsEnabled) {
                logImpression(
                        mMatchingKey,
                        mBucketingKey,
                        split,
                        resolvedTreatment.getTreatment(),
                        TreatmentLabels.EXCEPTION,
                        (evaluationResult != null) ? evaluationResult.getChangeNumber() : null,
                        mergedAttributes,
                        evaluationResult != null && evaluationResult.isImpressionsDisabled(),
                        evaluationOptions,
                        validationTag);
            }

            return new TreatmentResult(new SplitResult(resolvedTreatment.getTreatment(), resolvedTreatment.getConfig()), true);
        }
    }

    private void logImpression(String matchingKey, String bucketingKey, String splitName, String result, String label, Long changeNumber, Map<String, Object> attributes, boolean impressionsDisabled, EvaluationOptions evaluationOptions, String validationTag) {
        try {
            String propertiesJson = serializeProperties(evaluationOptions, validationTag);
            Impression impression = new Impression(matchingKey, bucketingKey, splitName, result, System.currentTimeMillis(), label, changeNumber, attributes, propertiesJson);
            DecoratedImpression decoratedImpression = new DecoratedImpression(impression, impressionsDisabled);
            mImpressionListener.log(decoratedImpression);
            mImpressionListener.log(impression);
        } catch (Throwable t) {
            Logger.e("An error occurred logging impression: " + t.getLocalizedMessage());
        }
    }

    @Nullable
    private String serializeProperties(@Nullable EvaluationOptions evaluationOptions, String validationTag) {
        if (evaluationOptions == null || evaluationOptions.getProperties() == null || evaluationOptions.getProperties().isEmpty()) {
            return null;
        }

        // validate using property validator
        PropertyValidator.Result result = mPropertyValidator.validate(evaluationOptions.getProperties(), validationTag);

        if (!result.isValid()) {
            mValidationLogger.e("Properties validation failed: " + (result.getErrorMessage() != null ? result.getErrorMessage() : "Unknown error"), validationTag);
            return null;
        }

        if (result.getProperties() == null || result.getProperties().isEmpty()) {
            return null;
        }

        try {
            return Json.toJson(result.getProperties());
        } catch (Exception e) {
            mValidationLogger.e("Failed to serialize properties to JSON: " + e.getLocalizedMessage(), validationTag);
            return null;
        }
    }

    @NonNull
    private <T> Map<String, T> getControlTreatmentsForSplitsWithConfig(@Nullable List<String> names, String validationTag, ResultTransformer<T> resultTransformer) {
        return TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(
                mSplitValidator,
                mValidationLogger,
                (names != null) ? names : new ArrayList<>(),
                validationTag,
                resultTransformer,
                mFallbackCalculator);
    }

    private EvaluationResult evaluateIfReady(String featureFlagName,
                                             Map<String, Object> attributes, String validationTag) {
        if (!mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY) &&
                !mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY_FROM_CACHE)) {
            mValidationLogger.w("the SDK is not ready, results may be incorrect for feature flag " + featureFlagName + ". Make sure to wait for SDK readiness before using this method", validationTag);
            mTelemetryStorageProducer.recordNonReadyUsage();

            FallbackTreatment fallback = mFallbackCalculator.resolve(featureFlagName, TreatmentLabels.NOT_READY);
            return new EvaluationResult(fallback.getTreatment(), fallback.getLabel(), null, fallback.getConfig(), false);
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

    private static class TreatmentResult {
        private final SplitResult mSplitResult;
        private final boolean mException;

        TreatmentResult(SplitResult splitResult, boolean exception) {
            mSplitResult = splitResult;
            mException = exception;
        }

        SplitResult getSplitResult() {
            return mSplitResult;
        }

        boolean isException() {
            return mException;
        }
    }
}
