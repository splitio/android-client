package io.split.android.client.validators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.EvaluationResult;
import io.split.android.client.Evaluator;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitResult;
import io.split.android.client.TreatmentLabels;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.utils.Logger;
import io.split.android.engine.metrics.Metrics;
import io.split.android.grammar.Treatments;

public class TreatmentManagerImpl implements TreatmentManager {

    private static class ValidationTag {
        public static final String GET_TREATMENT = "getTreatment";
        public static final String GET_TREATMENTS = "getTreatments";
        public static final String GET_TREATMENT_WITH_CONFIG = "getTreatmentWithConfig";
        public static final String GET_TREATMENTS_WITH_CONFIG = "getTreatmentsWithConfig";
    }

    private final String CLIENT_DESTROYED_MESSAGE = "Client has already been destroyed - no calls possible";
    private final String SDK_READY_NOT_FIRED = "No listeners for SDK Readiness detected. Incorrect control treatments could be logged if you call getTreatment while the SDK is not yet ready";

    private final Evaluator mEvaluator;
    private final KeyValidator mKeyValidator;
    private final SplitValidator mSplitValidator;
    private final Metrics mMetrics;
    private final ImpressionListener mImpressionListener;
    private final String mMatchingKey;
    private final String mBucketingKey;
    private final SplitClientConfig mSplitClientConfig;
    private final ValidationMessageLogger mValidationLogger;

    public TreatmentManagerImpl(String matchingKey, String bucketingKey,
                                Evaluator evaluator, KeyValidator keyValidator,
                                SplitValidator splitValidator, Metrics metrics,
                                ImpressionListener impressionListener, SplitClientConfig splitClientConfig) {
        mEvaluator = evaluator;
        mKeyValidator = keyValidator;
        mSplitValidator = splitValidator;
        mMetrics = metrics;
        mMatchingKey = matchingKey;
        mBucketingKey = bucketingKey;
        mImpressionListener = impressionListener;
        mSplitClientConfig = splitClientConfig;
        mValidationLogger = new ValidationMessageLoggerImpl();
    }

    @Override
    public String getTreatment(String split, Map<String, Object> attributes, boolean isClientDestroyed, boolean isSdkReadyFired) {

        final String validationTag = ValidationTag.GET_TREATMENT;
        if(isClientDestroyed) {
            Logger.e(validationTag + CLIENT_DESTROYED_MESSAGE);
            return Treatments.CONTROL;
        }

        if (!isSdkReadyFired) {
            mValidationLogger.e(SDK_READY_NOT_FIRED, validationTag);
            logImpression(
                    mMatchingKey,
                    mBucketingKey,
                    split,
                    Treatments.CONTROL,
                    (mSplitClientConfig.labelsEnabled() ? TreatmentLabels.NOT_READY : null),
                    null,
                    attributes
            );

            return Treatments.CONTROL;
        }

        long start = System.currentTimeMillis();
        String treatment = getTreatmentWithConfigWithoutMetrics(split, attributes, validationTag).treatment();
        mMetrics.time(Metrics.GET_TREATMENT_TIME, System.currentTimeMillis() - start);
        return treatment;
    }

    @Override
    public SplitResult getTreatmentWithConfig(String split, Map<String, Object> attributes, boolean isClientDestroyed, boolean isSdkReadyFired) {
        final String validationTag = ValidationTag.GET_TREATMENT_WITH_CONFIG;
        if(isClientDestroyed) {
            Logger.e(validationTag + CLIENT_DESTROYED_MESSAGE);
            return new SplitResult(Treatments.CONTROL);
        }

        if (!isSdkReadyFired) {
            mValidationLogger.e(SDK_READY_NOT_FIRED, validationTag);
            logImpression(
                    mMatchingKey,
                    mBucketingKey,
                    split,
                    Treatments.CONTROL,
                    (mSplitClientConfig.labelsEnabled() ? TreatmentLabels.NOT_READY : null),
                    null,
                    attributes
            );

            return new SplitResult(Treatments.CONTROL);
        }

        long start = System.currentTimeMillis();
        SplitResult result = getTreatmentWithConfigWithoutMetrics(split, attributes, validationTag);
        mMetrics.time(Metrics.GET_TREATMENT_WITH_CONFIG_TIME, System.currentTimeMillis() - start);
        return result;
    }

    @Override
    public Map<String, String> getTreatments(List<String> splits, Map<String, Object> attributes, boolean isClientDestroyed, boolean isSdkReadyFired) {

        final String validationTag = ValidationTag.GET_TREATMENTS;

        if(splits == null) {
            mValidationLogger.e("split_names must be a non-empty array", validationTag);
            return new HashMap<>();
        }

        if(isClientDestroyed) {
            mValidationLogger.e( CLIENT_DESTROYED_MESSAGE, validationTag);
            return controlTreatmentsForSplits(splits, validationTag);
        }

        if (!isSdkReadyFired) {
            mValidationLogger.e(SDK_READY_NOT_FIRED, validationTag);
            Map<String, String> controls = controlTreatmentsForSplits(splits, validationTag);
            logControlImpressions(new ArrayList(controls.keySet()), attributes, TreatmentLabels.NOT_READY);
            return controls;
        }

        long start = System.currentTimeMillis();
        Map<String, SplitResult> resultWithConfig = getTreatmentsWithConfigWithoutMetrics(splits, attributes ,validationTag);
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, SplitResult> entry : resultWithConfig.entrySet()) {
            result.put(entry.getKey(), entry.getValue().treatment());
        }
        mMetrics.time(Metrics.GET_TREATMENTS_TIME, System.currentTimeMillis() - start);
        return result;
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> splits, Map<String, Object> attributes, boolean isClientDestroyed, boolean isSdkReadyFired) {

        final String validationTag = ValidationTag.GET_TREATMENTS_WITH_CONFIG;

        if(splits == null) {
            mValidationLogger.e("split_names must be a non-empty array", validationTag);
            return new HashMap<>();
        }

        if(isClientDestroyed) {
            Logger.e(validationTag + CLIENT_DESTROYED_MESSAGE);
            return controlTreatmentsForSplitsWithConfig(splits, validationTag);
        }

        if (!isSdkReadyFired) {
            mValidationLogger.e(SDK_READY_NOT_FIRED, validationTag);
            Map<String, SplitResult> controls = controlTreatmentsForSplitsWithConfig(splits, validationTag);
            logControlImpressions(new ArrayList(controls.keySet()), attributes, TreatmentLabels.NOT_READY);
            return controls;
        }

        long start = System.currentTimeMillis();
        Map<String, SplitResult> result = getTreatmentsWithConfigWithoutMetrics(splits, attributes, validationTag);
        mMetrics.time(Metrics.GET_TREATMENTS_WITH_CONFIG_TIME, System.currentTimeMillis() - start);
        return result;
    }

    private SplitResult getTreatmentWithConfigWithoutMetrics(String split, Map<String, Object> attributes, String validationTag) {

        ValidationErrorInfo errorInfo = mKeyValidator.validate(mMatchingKey, mBucketingKey);
        if (errorInfo != null) {
            mValidationLogger.log(errorInfo, validationTag);
            return new SplitResult(Treatments.CONTROL);
        }

        String splitName = split;
        errorInfo = mSplitValidator.validateName(split);
        if (errorInfo != null) {
            mValidationLogger.log(errorInfo, validationTag);
            if (errorInfo.isError()) {
                return new SplitResult(Treatments.CONTROL);
            }
            splitName = split.trim();
        }

        EvaluationResult evaluationResult = mEvaluator.getTreatment(mMatchingKey, mBucketingKey, split, attributes);
        SplitResult splitResult = new SplitResult(evaluationResult.getTreatment(), evaluationResult.getConfigurations());

        if(evaluationResult.getLabel().equals(TreatmentLabels.DEFINITION_NOT_FOUND)) {
            mValidationLogger.w(mSplitValidator.splitNotFoundMessage(splitName), validationTag);
            return splitResult;
        }

        logImpression(
                mMatchingKey,
                mBucketingKey,
                splitName,
                evaluationResult.getTreatment(),
                (mSplitClientConfig.labelsEnabled() ? evaluationResult.getLabel() : null),
                evaluationResult.getChangeNumber(),
                attributes
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

        if(splits.size() == 0) {
            Logger.w(validationTag + ": split_names is an empty array or has null values");
            return results;
        }

        for(String split : splits) {
            errorInfo = mSplitValidator.validateName(split);
            if (errorInfo != null) {
                mValidationLogger.log(errorInfo, validationTag);
                if(errorInfo.isError()) {
                    continue;
                }
            }

            EvaluationResult result = mEvaluator.getTreatment(mMatchingKey, mBucketingKey, split.trim(), attributes);
            results.put(split.trim(), new SplitResult(result.getTreatment(), result.getConfigurations()));

            if(result.getLabel().equals(TreatmentLabels.DEFINITION_NOT_FOUND)) {
                mValidationLogger.w(mSplitValidator.splitNotFoundMessage(split), validationTag);
                continue;
            }

            logImpression(
                    mMatchingKey,
                    mBucketingKey,
                    split,
                    result.getTreatment(),
                    (mSplitClientConfig.labelsEnabled() ? result.getLabel() : null),
                    result.getChangeNumber(),
                    attributes);
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
        Map<String, SplitResult> results = new HashMap<>();
        for(String split : splits) {
            ValidationErrorInfo errorInfo = mSplitValidator.validateName(split);
            if(errorInfo != null) {
                mValidationLogger.log(errorInfo, validationTag);
                if(errorInfo.isError()) {
                    continue;
                }
            }
            results.put(split.trim(), new SplitResult(Treatments.CONTROL));
        }
        return results;
    }

    private Map<String, String> controlTreatmentsForSplits(List<String> splits, String validationTag) {
        Map<String, String> results = new HashMap<>();
        for(String split : splits) {
            ValidationErrorInfo errorInfo = mSplitValidator.validateName(split);
            if(errorInfo != null) {
                mValidationLogger.log(errorInfo, validationTag);
                if(errorInfo.isError()) {
                    continue;
                }
            }
            results.put(split.trim(), Treatments.CONTROL);
        }
        return results;
    }

    private void logControlImpressions(List<String> splits, Map<String, Object> attributes, String label) {
        for(String split : splits) {
            logImpression(
                    mMatchingKey,
                    mBucketingKey,
                    split,
                    Treatments.CONTROL,
                    label,
                    null,
                    attributes);
        }
    }

}
