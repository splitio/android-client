package io.split.android.client;

import com.google.common.base.Strings;

import io.split.android.client.api.Key;
import io.split.android.client.dtos.ConditionType;
import io.split.android.client.dtos.Event;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.exceptions.ChangeNumberExceptionWrapper;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.utils.Logger;
import io.split.android.client.validators.KeyValidator;
import io.split.android.client.validators.KeyValidatorImpl;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.engine.experiments.ParsedCondition;
import io.split.android.engine.experiments.ParsedSplit;
import io.split.android.engine.experiments.SplitFetcher;
import io.split.android.engine.metrics.Metrics;
import io.split.android.engine.splitter.Splitter;
import io.split.android.grammar.Treatments;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A basic implementation of SplitClient.
 *
 */
public final class SplitClientImpl implements SplitClient {

    private static final String NOT_IN_SPLIT = "not in split";
    private static final String DEFAULT_RULE = "default rule";
    private static final String DEFINITION_NOT_FOUND = "definition not found";
    private static final String EXCEPTION = "exception";
    private static final String KILLED = "killed";

    private final SplitFactory _container;
    private final SplitFetcher _splitFetcher;
    private final ImpressionListener _impressionListener;
    private final Metrics _metrics;
    private final SplitClientConfig _config;
    private final String _matchingKey;
    private final String _bucketingKey;

    private final SplitEventsManager _eventsManager;

    private final TrackClient _trackClient;

    private final KeyValidator _keyValidator = new KeyValidatorImpl();
    private final SplitValidator _splitValidator = new SplitValidatorImpl();

    private boolean _isClientDestroyed = false;

    public SplitClientImpl(SplitFactory container, Key key, SplitFetcher splitFetcher, ImpressionListener impressionListener, Metrics metrics, SplitClientConfig config, SplitEventsManager eventsManager, TrackClient trackClient) {
        _container = container;
        _splitFetcher = splitFetcher;
        _impressionListener = impressionListener;
        _metrics = metrics;
        _config = config;
        _matchingKey = key.matchingKey();
        _bucketingKey = key.bucketingKey();
        _eventsManager = eventsManager;
        _trackClient = trackClient;


        checkNotNull(_splitFetcher);
        checkNotNull(_impressionListener);
        checkNotNull(_matchingKey);
        checkNotNull(_eventsManager);
        checkNotNull(_trackClient);

    }

    @Override
    public void destroy() {
        _isClientDestroyed = true;
        _container.destroy();
    }

    @Override
    public void flush() {
        _container.flush();
    }

    @Override
    public boolean isReady() {
        return _container.isReady();
    }

    @Override
    public String getTreatment(String split) {
        return getTreatment(split, Collections.<String, Object>emptyMap());
    }

    @Override
    public String getTreatment(String split, Map<String, Object> attributes) {
        if(_isClientDestroyed) {
            Logger.e("Client has already been destroyed - no calls possible");
            return Treatments.CONTROL;
        }

        return getTreatment(_matchingKey, _bucketingKey, split, attributes);
    }

    @Override
    public Map<String, String> getTreatments(List<String> splits, Map<String, Object> attributes) {

        final String validationTag = "getTreatments";

        Map<String, String> results = new HashMap<>();
        if(_isClientDestroyed){
            Logger.e(validationTag + ": client has already been destroyed - no calls possible");
            for(String split : splits) {
                if (_splitValidator.isValidName(split, validationTag)) {
                    results.put(_splitValidator.trimName(split, validationTag), Treatments.CONTROL);
                }
            }
            return results;
        }

        if(splits == null) {
            Logger.e(validationTag + ": split_names cannot be null");
            return results;
        }

        if(splits.size() == 0) {
            Logger.w(validationTag + ": split_names is an empty array or has null values");
            return results;
        }

        for(String split : splits) {
            if (_splitValidator.isValidName(split, validationTag)) {
                results.put(split, getTreatment(_matchingKey, _bucketingKey, split, attributes, false));
            } else {
                results.put(_splitValidator.trimName(split, validationTag), Treatments.CONTROL);
            }
        }
        return results;
    }

    private String getTreatment(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes) {
        return getTreatment(matchingKey, bucketingKey, split, attributes, true);
    }

    private String getTreatment(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes, boolean validateInput) {
        try {

            String splitName = split;
            if (validateInput) {
                final String validationTag = "getTreatment";

                if (!_keyValidator.isValidKey(matchingKey, bucketingKey, validationTag)) {
                    return Treatments.CONTROL;
                }

                if (!_splitValidator.isValidName(split, validationTag)) {
                    return Treatments.CONTROL;
                }

                if (!_eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)) {
                    Logger.w("No listeners for SDK Readiness detected. Incorrect control treatments could be logged if you call getTreatment while the SDK is not yet ready");
                }

                splitName = _splitValidator.trimName(split, validationTag);
            }

            long start = System.currentTimeMillis();

            TreatmentLabelAndChangeNumber result = null;

            try {
                result = getTreatmentWithoutExceptionHandling(matchingKey, bucketingKey, splitName, attributes);
            } catch (ChangeNumberExceptionWrapper e) {
                result = new TreatmentLabelAndChangeNumber(Treatments.CONTROL, EXCEPTION, e.changeNumber());
                Logger.e(e.wrappedException());
            } catch (Exception e) {
                result = new TreatmentLabelAndChangeNumber(Treatments.CONTROL, EXCEPTION);
                Logger.e(e);
            }

            recordStats(
                    matchingKey,
                    bucketingKey,
                    splitName,
                    start,
                    result._treatment,
                    "sdk.getTreatment",
                    _config.labelsEnabled() ? result._label : null,
                    result._changeNumber,
                    attributes
            );

            return result._treatment;
        } catch (Exception e) {
            try {
                Logger.e(e, "CatchAll Exception");
            } catch (Exception e1) {
                // ignore
            }
            return Treatments.CONTROL;
        }
    }

    private void recordStats(String matchingKey, String bucketingKey, String split, long start, String result,
                             String operation, String label, Long changeNumber, Map<String, Object> attributes) {
        try {
            _impressionListener.log(new Impression(matchingKey, bucketingKey, split, result, System.currentTimeMillis(), label, changeNumber, attributes));
            _metrics.time(operation, System.currentTimeMillis() - start);
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    public String getTreatmentWithoutImpressions(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes) {
        return getTreatmentResultWithoutImpressions(matchingKey, bucketingKey, split, attributes)._treatment;
    }

    private TreatmentLabelAndChangeNumber getTreatmentResultWithoutImpressions(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes) {
        TreatmentLabelAndChangeNumber result;

        if (Strings.isNullOrEmpty(matchingKey)) {
            Logger.e("getTreatment: key cannot be null");
            return new TreatmentLabelAndChangeNumber(Treatments.CONTROL, EXCEPTION);
        }

        if (Strings.isNullOrEmpty(bucketingKey)) {
            Logger.w("getTreatment: Key object should have bucketingKey set");
        }

        try {
            result = getTreatmentWithoutExceptionHandling(matchingKey, bucketingKey, split, attributes);
        } catch (ChangeNumberExceptionWrapper e) {
            result = new TreatmentLabelAndChangeNumber(Treatments.CONTROL, EXCEPTION, e.changeNumber());
            Logger.e(e.wrappedException());
        } catch (Exception e) {
            result = new TreatmentLabelAndChangeNumber(Treatments.CONTROL, EXCEPTION);
            Logger.e(e);
        }

        return result;
    }

    private TreatmentLabelAndChangeNumber getTreatmentWithoutExceptionHandling(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes) throws ChangeNumberExceptionWrapper {
        ParsedSplit parsedSplit = _splitFetcher.fetch(split);

        if (parsedSplit == null) {
            Logger.d("Returning control because no split was found for: %s", split);
            return new TreatmentLabelAndChangeNumber(Treatments.CONTROL, DEFINITION_NOT_FOUND);
        }

        return getTreatment(matchingKey, bucketingKey, parsedSplit, attributes);
    }

    /**
     * @param matchingKey  MUST NOT be null
     * @param bucketingKey
     * @param parsedSplit  MUST NOT be null
     * @param attributes   MUST NOT be null
     * @return
     * @throws ChangeNumberExceptionWrapper
     */
    private TreatmentLabelAndChangeNumber getTreatment(String matchingKey, String bucketingKey, ParsedSplit parsedSplit, Map<String, Object> attributes) throws ChangeNumberExceptionWrapper {
        try {
            if (parsedSplit.killed()) {
                return new TreatmentLabelAndChangeNumber(parsedSplit.defaultTreatment(), KILLED, parsedSplit.changeNumber());
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
                            return new TreatmentLabelAndChangeNumber(parsedSplit.defaultTreatment(), NOT_IN_SPLIT, parsedSplit.changeNumber());
                        }

                    }
                    inRollout = true;
                }

                if (parsedCondition.matcher().match(matchingKey, bucketingKey, attributes, this)) {
                    String treatment = Splitter.getTreatment(bk, parsedSplit.seed(), parsedCondition.partitions(), parsedSplit.algo());
                    return new TreatmentLabelAndChangeNumber(treatment, parsedCondition.label(), parsedSplit.changeNumber());
                }
            }

            return new TreatmentLabelAndChangeNumber(parsedSplit.defaultTreatment(), DEFAULT_RULE, parsedSplit.changeNumber());
        } catch (Exception e) {
            throw new ChangeNumberExceptionWrapper(e, parsedSplit.changeNumber());
        }

    }

    private static final class TreatmentLabelAndChangeNumber {
        private final String _treatment;
        private final String _label;
        private final Long _changeNumber;

        public TreatmentLabelAndChangeNumber(String treatment, String label) {
            this(treatment, label, null);
        }

        public TreatmentLabelAndChangeNumber(String treatment, String label, Long changeNumber) {
            _treatment = treatment;
            _label = label;
            _changeNumber = changeNumber;
        }
    }


    public void on(SplitEvent event, SplitEventTask task){
        checkNotNull(event);
        checkNotNull(task);

        if(_eventsManager.eventAlreadyTriggered(event)) {
            Logger.w(String.format("A listener was added for %s on the SDK, which has already fired and won’t be emitted again. The callback won’t be executed.", event.toString()));
            return;
        }

        _eventsManager.register(event, task);
    }

    @Override
    public boolean track(String trafficType, String eventType) {
        return track(_matchingKey, trafficType, eventType);
    }

    @Override
    public boolean track(String trafficType, String eventType, double value) {
        return track(_matchingKey, trafficType, eventType, value);
    }

    @Override
    public boolean track(String eventType) {
        return track(_matchingKey, _config.trafficType(), eventType);
    }

    @Override
    public boolean track(String eventType, double value) {
        return track(_matchingKey, _config.trafficType(), eventType, value);
    }

    private boolean track(String key, String trafficType, String eventType) {
        return track(key, trafficType, eventType, 0.0);
    }

    private boolean track(String key, String trafficType, String eventType, double value) {

        if(_isClientDestroyed) {
            Logger.e("Client has already been destroyed - no calls possible");
            return false;
        }

        Event event = new Event();
        event.eventTypeId = eventType;
        event.trafficTypeName = trafficType;
        event.key = key;
        event.value = value;
        event.timestamp = System.currentTimeMillis();

        return _trackClient.track(event);
    }

}
