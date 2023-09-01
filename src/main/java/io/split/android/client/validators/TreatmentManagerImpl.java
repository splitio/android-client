package io.split.android.client.validators;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.EvaluationResult;
import io.split.android.client.Evaluator;
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

    private final String CLIENT_DESTROYED_MESSAGE = "Client has already been destroyed - no calls possible";

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
    private final Set<String> mConfiguredFlagSets;
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
                                @NonNull Set<String> configuredFlagSets,
                                @NonNull SplitsStorage splitsStorage) {
        mEvaluator = evaluator;
        mKeyValidator = keyValidator;
        mSplitValidator = splitValidator;
        mMatchingKey = matchingKey;
        mBucketingKey = bucketingKey;
        mImpressionListener = impressionListener;
        mLabelsEnabled = labelsEnabled;
        mEventsManager = eventsManager;
        mValidationLogger = new ValidationMessageLoggerImpl();
        mAttributesManager = checkNotNull(attributesManager);
        mAttributesMerger = checkNotNull(attributesMerger);
        mTelemetryStorageProducer = checkNotNull(telemetryStorageProducer);
        mConfiguredFlagSets = checkNotNull(configuredFlagSets);
        mSplitsStorage = checkNotNull(splitsStorage);
        mFlagSetsValidator = new FlagSetsValidatorImpl();
    }

    @Override
    public String getTreatment(String split, Map<String, Object> attributes, boolean isClientDestroyed) {

        final String validationTag = ValidationTag.GET_TREATMENT;
        if (isClientDestroyed) {
            Logger.e(validationTag + CLIENT_DESTROYED_MESSAGE);
            return Treatments.CONTROL;
        }

        long start = System.currentTimeMillis();

        String treatment = getTreatmentWithConfigWithoutMetrics(split, attributes, validationTag).treatment();

        recordLatency(Method.TREATMENT, start);

        return treatment;
    }

    @Override
    public SplitResult getTreatmentWithConfig(String split, Map<String, Object> attributes, boolean isClientDestroyed) {
        final String validationTag = ValidationTag.GET_TREATMENT_WITH_CONFIG;
        if (isClientDestroyed) {
            Logger.e(validationTag + CLIENT_DESTROYED_MESSAGE);
            return new SplitResult(Treatments.CONTROL);
        }

        long start = System.currentTimeMillis();

        SplitResult result = getTreatmentWithConfigWithoutMetrics(split, attributes, validationTag);

        recordLatency(Method.TREATMENT_WITH_CONFIG, start);

        return result;
    }

    @Override
    public Map<String, String> getTreatments(List<String> splits, Map<String, Object> attributes, boolean isClientDestroyed) {

        final String validationTag = ValidationTag.GET_TREATMENTS;

        if (splits == null) {
            mValidationLogger.e("split_names must be a non-empty array", validationTag);
            return new HashMap<>();
        }

        if (isClientDestroyed) {
            mValidationLogger.e(CLIENT_DESTROYED_MESSAGE, validationTag);
            return controlTreatmentsForSplits(splits, validationTag);
        }

        long start = System.currentTimeMillis();

        Map<String, SplitResult> resultWithConfig = getTreatmentsWithConfigWithoutMetrics(splits, attributes, validationTag);
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, SplitResult> entry : resultWithConfig.entrySet()) {
            result.put(entry.getKey(), entry.getValue().treatment());
        }

        recordLatency(Method.TREATMENTS, start);

        return result;
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> splits, Map<String, Object> attributes, boolean isClientDestroyed) {

        final String validationTag = ValidationTag.GET_TREATMENTS_WITH_CONFIG;

        if (splits == null) {
            mValidationLogger.e("split_names must be a non-empty array", validationTag);
            return new HashMap<>();
        }

        if (isClientDestroyed) {
            Logger.e(validationTag + CLIENT_DESTROYED_MESSAGE);
            return controlTreatmentsForSplitsWithConfig(splits, validationTag);
        }

        long start = System.currentTimeMillis();

        Map<String, SplitResult> result = getTreatmentsWithConfigWithoutMetrics(splits, attributes, validationTag);

        recordLatency(Method.TREATMENTS_WITH_CONFIG, start);

        return result;
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes, boolean isClientDestroyed) {
        String validationTag = ValidationTag.GET_TREATMENTS_BY_FLAG_SET;
        if (isClientDestroyed) {
            mValidationLogger.e(CLIENT_DESTROYED_MESSAGE, validationTag);
            return new HashMap<>();
        }

        if (!mFlagSetsValidator.isValid(flagSet)) {
            mValidationLogger.e("you passed " + flagSet + " which is not valid.", validationTag);
            return new HashMap<>();
        }

        if (!mConfiguredFlagSets.isEmpty() && !mConfiguredFlagSets.contains(flagSet)) {
            mValidationLogger.e("you passed " + flagSet + " which is not defined in the configuration.", validationTag);
            return new HashMap<>();
        }

        long start = System.currentTimeMillis();

        Map<String, String> result = new HashMap<>();
        Set<String> featureFlagNamesInSet = mSplitsStorage.getNamesByFlagSets(Collections.singletonList(flagSet));
        for (String featureFlagName : featureFlagNamesInSet) {
            result.put(featureFlagName, getTreatmentWithConfigWithoutMetrics(featureFlagName, attributes, validationTag).treatment());
        }

        recordLatency(Method.TREATMENTS_BY_FLAG_SET, start);

        return result;
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes, boolean isClientDestroyed) {
        return null;
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes, boolean isClientDestroyed) {
        return null;
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes, boolean isClientDestroyed) {
        return null;
    }

    private SplitResult getTreatmentWithConfigWithoutMetrics(String split, Map<String, Object> attributes, String validationTag) {

        ValidationErrorInfo errorInfo = mKeyValidator.validate(mMatchingKey, mBucketingKey);
        if (errorInfo != null) {
            mValidationLogger.e(errorInfo, validationTag);
            return new SplitResult(Treatments.CONTROL);
        }
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

        final Map<String, Object> mergedAttributes = mAttributesMerger.merge(mAttributesManager.getAllAttributes(), attributes);
        EvaluationResult evaluationResult = evaluateIfReady(splitName, mergedAttributes, validationTag);
        SplitResult splitResult = new SplitResult(evaluationResult.getTreatment(), evaluationResult.getConfigurations());

        if (evaluationResult.getLabel().equals(TreatmentLabels.DEFINITION_NOT_FOUND)) {
            mValidationLogger.w(mSplitValidator.splitNotFoundMessage(splitName), validationTag);
            return splitResult;
        }

        logImpression(
                mMatchingKey,
                mBucketingKey,
                splitName,
                evaluationResult.getTreatment(),
                (mLabelsEnabled ? evaluationResult.getLabel() : null),
                evaluationResult.getChangeNumber(),
                mergedAttributes
        );

        return splitResult;
    }

    private Map<String, SplitResult> getTreatmentsWithConfigWithoutMetrics(List<String> splits, Map<String, Object> attributes, String validationTag) {

        ValidationErrorInfo errorInfo = mKeyValidator.validate(mMatchingKey, mBucketingKey);
        if (errorInfo != null) {
            mValidationLogger.log(errorInfo, validationTag);
            return controlTreatmentsForSplitsWithConfig(splits, validationTag);
        }

        Map<String, SplitResult> results = new HashMap<>();

        if (splits.size() == 0) {
            Logger.w(validationTag + ": split_names is an empty array or has null values");
            return results;
        }

        final Map<String, Object> mergedAttributes = mAttributesMerger.merge(mAttributesManager.getAllAttributes(), attributes);
        for (String split : splits) {
            errorInfo = mSplitValidator.validateName(split);
            if (errorInfo != null) {
                if (errorInfo.isError()) {
                    mValidationLogger.e(errorInfo, validationTag);
                    continue;
                }
                mValidationLogger.w(errorInfo, validationTag);
            }

            EvaluationResult result = evaluateIfReady(split.trim(), mergedAttributes, validationTag);
            results.put(split.trim(), new SplitResult(result.getTreatment(), result.getConfigurations()));

            if (result.getLabel().equals(TreatmentLabels.DEFINITION_NOT_FOUND)) {
                mValidationLogger.w(mSplitValidator.splitNotFoundMessage(split), validationTag);
                continue;
            }

            logImpression(
                    mMatchingKey,
                    mBucketingKey,
                    split,
                    result.getTreatment(),
                    (mLabelsEnabled ? result.getLabel() : null),
                    result.getChangeNumber(),
                    mergedAttributes);
        }

        return results;
    }

    private void logImpression(String matchingKey, String bucketingKey, String splitName, String result, String label, Long changeNumber, Map<String, Object> attributes) {
        try {
            mImpressionListener.log(new Impression(matchingKey, bucketingKey, splitName, result, System.currentTimeMillis(), label, changeNumber, attributes));
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private Map<String, SplitResult> controlTreatmentsForSplitsWithConfig(List<String> splits, String validationTag) {
        return TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(splits, mSplitValidator, validationTag, mValidationLogger);
    }

    @SuppressWarnings("SameParameterValue")
    private Map<String, String> controlTreatmentsForSplits(List<String> splits, String validationTag) {
        return TreatmentManagerHelper.controlTreatmentsForSplits(splits, mSplitValidator, validationTag, mValidationLogger);
    }

    private EvaluationResult evaluateIfReady(String splitName,
                                             Map<String, Object> attributes, String validationTag) {
        if (!mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY) &&
                !mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY_FROM_CACHE)) {
            mValidationLogger.w("the SDK is not ready, results may be incorrect. Make sure to wait for SDK readiness before using this method", validationTag);
            mTelemetryStorageProducer.recordNonReadyUsage();

            return new EvaluationResult(Treatments.CONTROL, TreatmentLabels.NOT_READY, null, null);
        }
        return mEvaluator.getTreatment(mMatchingKey, mBucketingKey, splitName, attributes);
    }

    private void recordLatency(Method treatment, long startTime) {
        mTelemetryStorageProducer.recordLatency(treatment, System.currentTimeMillis() - startTime);
    }
}
