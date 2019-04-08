package io.split.android.client.validators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.EvaluationResult;
import io.split.android.client.Evaluator;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitResult;
import io.split.android.client.events.SplitEvent;
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
    }

    @Override
    public String getTreatment(String split, Map<String, Object> attributes, boolean isClientDestroyed, boolean isSdkReadyFired) {

        final String validationTag = ValidationTag.GET_TREATMENT;
        if(isClientDestroyed) {
            Logger.e(validationTag + CLIENT_DESTROYED_MESSAGE);
            return Treatments.CONTROL;
        }

        long start = System.currentTimeMillis();
        String treatment = getTreatmentWithConfigWithoutMetrics(split, attributes, validationTag, isSdkReadyFired).getTreatment();
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
        long start = System.currentTimeMillis();
        SplitResult result = getTreatmentWithConfigWithoutMetrics(split, attributes, validationTag, isSdkReadyFired);
        mMetrics.time(Metrics.GET_TREATMENT_WITH_CONFIG_TIME, System.currentTimeMillis() - start);
        return result;
    }

    @Override
    public Map<String, String> getTreatments(List<String> splits, Map<String, Object> attributes, boolean isClientDestroyed, boolean isSdkReadyFired) {

        final String validationTag = ValidationTag.GET_TREATMENTS;

        if(splits == null) {
            Logger.e(validationTag + ": split_names must be a non-empty array");
            return new HashMap<>();
        }

        if(isClientDestroyed) {
            Logger.e(validationTag + CLIENT_DESTROYED_MESSAGE);
            return controlTreatmentsForSplits(splits, validationTag);
        }

        long start = System.currentTimeMillis();
        Map<String, SplitResult> resultWithConfig = getTreatmentsWithConfigWithoutMetrics(splits, attributes ,validationTag, isSdkReadyFired);
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, SplitResult> entry : resultWithConfig.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getTreatment());
        }
        mMetrics.time(Metrics.GET_TREATMENTS_TIME, System.currentTimeMillis() - start);
        return result;
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> splits, Map<String, Object> attributes, boolean isClientDestroyed, boolean isSdkReadyFired) {

        final String validationTag = ValidationTag.GET_TREATMENTS_WITH_CONFIG;

        if(splits == null) {
            Logger.e(validationTag + ": split_names must be a non-empty array");
            return new HashMap<>();
        }

        if(isClientDestroyed) {
            Logger.e(validationTag + CLIENT_DESTROYED_MESSAGE);
            return controlTreatmentsForSplitsWithConfig(splits, validationTag);
        }

        long start = System.currentTimeMillis();
        Map<String, SplitResult> result = getTreatmentsWithConfigWithoutMetrics(splits, attributes, validationTag, isSdkReadyFired);
        mMetrics.time(Metrics.GET_TREATMENTS_WITH_CONFIG_TIME, System.currentTimeMillis() - start);
        return result;
    }

    private SplitResult getTreatmentWithConfigWithoutMetrics(String split, Map<String, Object> attributes, String validationTag, boolean isSdkReadyFired) {

        String splitName = split;
        if (!isSdkReadyFired) {
            Logger.w(validationTag + SDK_READY_NOT_FIRED);
        }

        if (!mKeyValidator.isValidKey(mMatchingKey, mBucketingKey, validationTag)) {
            return new SplitResult(Treatments.CONTROL);
        }

        if (!mSplitValidator.isValidName(split, validationTag)) {
            return new SplitResult(Treatments.CONTROL);
        }

        splitName = mSplitValidator.trimName(split, validationTag);

        EvaluationResult evaluationResult = mEvaluator.getTreatment(mMatchingKey, mBucketingKey, split, attributes);
        SplitResult splitResult = new SplitResult(evaluationResult.getTreatment(), evaluationResult.getConfigurations());

        logImpression(
                mMatchingKey,
                mBucketingKey,
                splitName,
                evaluationResult.getTreatment(),
                "sdk.getTreatment",
                mSplitClientConfig.labelsEnabled() ? evaluationResult.getLabel() : null,
                evaluationResult.getChangeNumber(),
                attributes
        );

        return splitResult;
    }

    private Map<String, SplitResult> getTreatmentsWithConfigWithoutMetrics(List<String> splits, Map<String, Object> attributes, String validationTag, boolean isSdkReadyFired) {

        if (!isSdkReadyFired) {
            Logger.w(validationTag + SDK_READY_NOT_FIRED);
        }

        if (!mKeyValidator.isValidKey(mMatchingKey, mBucketingKey, validationTag)) {
            return controlTreatmentsForSplitsWithConfig(splits, validationTag);
        }

        Map<String, SplitResult> results = new HashMap<>();

        if(splits.size() == 0) {
            Logger.w(validationTag + ": split_names is an empty array or has null values");
            return results;
        }

        for(String split : splits) {
            if (mSplitValidator.isValidName(split, validationTag)) {
                EvaluationResult result = mEvaluator.getTreatment(mMatchingKey, mBucketingKey, mSplitValidator.trimName(split, validationTag), attributes);
                results.put(split.trim(), new SplitResult(result.getTreatment(), result.getConfigurations()));
            }
        }

        return results;
    }

    private void logImpression(String matchingKey, String bucketingKey, String splitName, String result, String operation, String label, Long changeNumber, Map<String, Object> attributes) {
        try {
            mImpressionListener.log(new Impression(matchingKey, bucketingKey, splitName, result, System.currentTimeMillis(), label, changeNumber, attributes));
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private Map<String, SplitResult> controlTreatmentsForSplitsWithConfig(List<String> splits, String validationTag) {
        Map<String, SplitResult> results = new HashMap<>();
        for(String split : splits) {
            if(mSplitValidator.isValidName(split, validationTag)) {
                results.put(mSplitValidator.trimName(split, validationTag), new SplitResult(Treatments.CONTROL));
            }
        }
        return results;
    }

    private Map<String, String> controlTreatmentsForSplits(List<String> splits, String validationTag) {
        Map<String, String> results = new HashMap<>();
        for(String split : splits) {
            if(mSplitValidator.isValidName(split, validationTag)) {
                results.put(mSplitValidator.trimName(split, validationTag), Treatments.CONTROL);
            }
        }
        return results;
    }

}
