package io.split.android.client;

import java.util.Map;

import io.split.android.client.dtos.ConditionType;
import io.split.android.client.exceptions.ChangeNumberExceptionWrapper;
import io.split.android.client.fallback.FallbackConfiguration;
import io.split.android.client.fallback.FallbackTreatment;
import io.split.android.client.fallback.FallbackTreatmentsCalculator;
import io.split.android.client.fallback.FallbackTreatmentsCalculatorImpl;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.logger.Logger;
import io.split.android.engine.experiments.ParsedCondition;
import io.split.android.engine.experiments.ParsedSplit;
import io.split.android.engine.experiments.SplitParser;
import io.split.android.engine.matchers.PrerequisitesMatcher;
import io.split.android.engine.splitter.Splitter;

public class EvaluatorImpl implements Evaluator {

    private final SplitsStorage mSplitsStorage;
    private final SplitParser mSplitParser;
    private final FallbackTreatmentsCalculator mFallbackCalculator;

    public EvaluatorImpl(SplitsStorage splitsStorage, SplitParser splitParser) {
        this(splitsStorage, splitParser, new FallbackTreatmentsCalculatorImpl(FallbackConfiguration.builder().build()));
    }

    public EvaluatorImpl(SplitsStorage splitsStorage, SplitParser splitParser, FallbackTreatmentsCalculator fallbackCalculator) {
        mSplitsStorage = splitsStorage;
        mSplitParser = splitParser;
        mFallbackCalculator = fallbackCalculator;
    }

    @Override
    public EvaluationResult getTreatment(String matchingKey, String bucketingKey, String splitName, Map<String, Object> attributes) {

        try {
            ParsedSplit parsedSplit = mSplitParser.parse(mSplitsStorage.get(splitName), matchingKey);
            if (parsedSplit == null) {
                FallbackTreatment fallback = mFallbackCalculator.resolve(splitName, TreatmentLabels.DEFINITION_NOT_FOUND);
                return new EvaluationResult(fallback.getTreatment(), fallback.getLabel(), null, fallback.getConfig(), true);
            }

            return getTreatment(matchingKey, bucketingKey, parsedSplit, attributes);
        } catch (ChangeNumberExceptionWrapper ex) {
            Logger.e(ex, "Catch Change Number Exception");
            FallbackTreatment fallback = mFallbackCalculator.resolve(splitName, TreatmentLabels.EXCEPTION);
            return new EvaluationResult(fallback.getTreatment(), fallback.getLabel(), ex.changeNumber(), fallback.getConfig(), true);
        } catch (Exception e) {
            Logger.e(e, "Catch All Exception");
            FallbackTreatment fallback = mFallbackCalculator.resolve(splitName, TreatmentLabels.EXCEPTION);
            return new EvaluationResult(fallback.getTreatment(), fallback.getLabel(), null, fallback.getConfig(), true);
        }
    }

    /**
     * @param matchingKey  MUST NOT be null
     * @param bucketingKey
     * @param parsedSplit  MUST NOT be null
     * @param attributes   MUST NOT be null
     * @return
     * @throws ChangeNumberExceptionWrapper
     */
    private EvaluationResult getTreatment(String matchingKey, String bucketingKey, ParsedSplit parsedSplit, Map<String, Object> attributes) throws ChangeNumberExceptionWrapper {
        try {
            if (parsedSplit.killed()) {
                return new EvaluationResult(parsedSplit.defaultTreatment(), TreatmentLabels.KILLED, parsedSplit.changeNumber(), configForTreatment(parsedSplit, parsedSplit.defaultTreatment()), parsedSplit.impressionsDisabled());
            }

            if (!parsedSplit.prerequisites().isEmpty()) {
                PrerequisitesMatcher matcher = new PrerequisitesMatcher(parsedSplit.prerequisites());
                if (!matcher.match(matchingKey, bucketingKey, attributes, this)) {
                    return new EvaluationResult(parsedSplit.defaultTreatment(),
                            TreatmentLabels.PREREQUISITES_NOT_MET,
                            parsedSplit.changeNumber(),
                            configForTreatment(parsedSplit, parsedSplit.defaultTreatment()),
                            parsedSplit.impressionsDisabled());
                }
            }

            /*
             * There are three parts to a single Split: 1) Whitelists 2) Traffic Allocation
             * 3) Rollout. The flag inRollout is there to understand when we move into the Rollout
             * section. This is because we need to make sure that the Traffic Allocation
             * computation happens after the whitelist but before the rollout.
             */
            boolean inRollout = false;

            String bk = (bucketingKey == null) ? matchingKey : bucketingKey;

            for (ParsedCondition parsedCondition : parsedSplit.parsedConditions()) {

                if (!inRollout && parsedCondition.conditionType() == ConditionType.ROLLOUT) {

                    if (parsedSplit.trafficAllocation() < 100) {
                        // if the traffic allocation is 100%, no need to do anything special.
                        int bucket = Splitter.getBucket(bk, parsedSplit.trafficAllocationSeed(), parsedSplit.algo());

                        if (bucket > parsedSplit.trafficAllocation()) {
                            // out of split
                            return new EvaluationResult(parsedSplit.defaultTreatment(), TreatmentLabels.NOT_IN_SPLIT, parsedSplit.changeNumber(), configForTreatment(parsedSplit, parsedSplit.defaultTreatment()), parsedSplit.impressionsDisabled());
                        }

                    }
                    inRollout = true;
                }

                if (parsedCondition.matcher().match(matchingKey, bucketingKey, attributes, this)) {
                    String treatment = Splitter.getTreatment(bk, parsedSplit.seed(), parsedCondition.partitions(), parsedSplit.algo(), mFallbackCalculator);
                    return new EvaluationResult(treatment, parsedCondition.label(), parsedSplit.changeNumber(), configForTreatment(parsedSplit, treatment), parsedSplit.impressionsDisabled());
                }
            }

            return new EvaluationResult(parsedSplit.defaultTreatment(), TreatmentLabels.DEFAULT_RULE, parsedSplit.changeNumber(), configForTreatment(parsedSplit, parsedSplit.defaultTreatment()), parsedSplit.impressionsDisabled());
        } catch (Exception e) {
            throw new ChangeNumberExceptionWrapper(e, parsedSplit.changeNumber());
        }
    }

    private String configForTreatment(ParsedSplit split, String treatment) {
        String config = null;
        if (split.configurations() != null) {
            config = split.configurations().get(treatment);
        }
        return config;
    }
}
