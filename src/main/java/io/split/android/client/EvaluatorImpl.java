package io.split.android.client;

import java.util.Map;

import io.split.android.client.dtos.ConditionType;
import io.split.android.client.exceptions.ChangeNumberExceptionWrapper;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.storage.TelemetryEvaluationProducer;
import io.split.android.client.utils.Logger;
import io.split.android.engine.experiments.ParsedCondition;
import io.split.android.engine.experiments.ParsedSplit;
import io.split.android.engine.experiments.SplitParser;
import io.split.android.engine.splitter.Splitter;
import io.split.android.grammar.Treatments;

public class EvaluatorImpl implements Evaluator {

    private final SplitsStorage mSplitsStorage;
    private final SplitParser mSplitParser;
    private final TelemetryEvaluationProducer mTelemetryEvaluationProducer;

    public EvaluatorImpl(SplitsStorage splitsStorage, SplitParser splitParser, TelemetryEvaluationProducer telemetryEvaluationProducer) {
        mSplitsStorage = splitsStorage;
        mSplitParser = splitParser;
        mTelemetryEvaluationProducer = telemetryEvaluationProducer;
    }

    @Override
    public EvaluationResult getTreatment(String matchingKey, String bucketingKey, String splitName, Map<String, Object> attributes, Method callingMethod) {

        try {
            ParsedSplit parsedSplit = mSplitParser.parse(mSplitsStorage.get(splitName));
            if (parsedSplit == null) {
                return new EvaluationResult(Treatments.CONTROL, TreatmentLabels.DEFINITION_NOT_FOUND);
            }

            return getTreatment(matchingKey, bucketingKey, parsedSplit, attributes);
        } catch (ChangeNumberExceptionWrapper ex) {
            Logger.e(ex, "Catch Change Number Exception");
        } catch (Exception e) {
            Logger.e(e, "Catch All Exception");
        }
        mTelemetryEvaluationProducer.recordException(callingMethod);
        return new EvaluationResult(Treatments.CONTROL, TreatmentLabels.EXCEPTION);
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
                return new EvaluationResult(parsedSplit.defaultTreatment(), TreatmentLabels.KILLED, parsedSplit.changeNumber(), configForTreatment(parsedSplit, parsedSplit.defaultTreatment()));
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
                            return new EvaluationResult(parsedSplit.defaultTreatment(), TreatmentLabels.NOT_IN_SPLIT, parsedSplit.changeNumber(), configForTreatment(parsedSplit, parsedSplit.defaultTreatment()));
                        }

                    }
                    inRollout = true;
                }

                if (parsedCondition.matcher().match(matchingKey, bucketingKey, attributes, this)) {
                    String treatment = Splitter.getTreatment(bk, parsedSplit.seed(), parsedCondition.partitions(), parsedSplit.algo());
                    return new EvaluationResult(treatment, parsedCondition.label(), parsedSplit.changeNumber(), configForTreatment(parsedSplit, treatment));
                }
            }

            return new EvaluationResult(parsedSplit.defaultTreatment(), TreatmentLabels.DEFAULT_RULE, parsedSplit.changeNumber(), configForTreatment(parsedSplit, parsedSplit.defaultTreatment()));
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
