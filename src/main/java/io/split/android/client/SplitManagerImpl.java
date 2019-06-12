package io.split.android.client;

import io.split.android.client.api.SplitView;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Split;
import io.split.android.client.utils.Logger;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.client.validators.ValidationErrorInfo;
import io.split.android.client.validators.ValidationMessageLogger;
import io.split.android.client.validators.ValidationMessageLoggerImpl;
import io.split.android.engine.experiments.ParsedCondition;
import io.split.android.engine.experiments.ParsedSplit;
import io.split.android.engine.experiments.SplitFetcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SplitManagerImpl implements SplitManager {

    private final SplitFetcher _splitFetcher;
    private boolean _isManagerDestroyed = false;
    private SplitValidator _splitValidator;
    private ValidationMessageLogger _validationMessageLogger;


    public SplitManagerImpl(SplitFetcher splitFetcher) {
        _validationMessageLogger = new ValidationMessageLoggerImpl();
        _splitFetcher  = splitFetcher;
        _splitValidator = new SplitValidatorImpl(splitFetcher);
    }

    @Override
    public List<SplitView> splits() {
        List<SplitView> result = new ArrayList<>();

        if(_isManagerDestroyed){
            Logger.e("Manager has already been destroyed - no calls possible");
            return result;
        }

        List<ParsedSplit> parsedSplits = _splitFetcher.fetchAll();
        for (ParsedSplit split : parsedSplits) {
            result.add(toSplitView(split));
        }
        return result;
    }

    @Override
    public SplitView split(String featureName) {

        final String validationTag = "split";
        String splitName = featureName;

        if(_isManagerDestroyed){
            Logger.e("Manager has already been destroyed - no calls possible");
            return null;
        }

        ValidationErrorInfo errorInfo = _splitValidator.validateName(featureName);
        if (errorInfo != null) {
            _validationMessageLogger.log(errorInfo, validationTag);
            if(errorInfo.isError()) {
                return null;
            }
            splitName = featureName.trim();
        }

        ParsedSplit parsedSplit = _splitFetcher.fetch(splitName);
        if(parsedSplit == null) {
            _validationMessageLogger.w(_splitValidator.splitNotFoundMessage(splitName), validationTag);
            return null;
        }
        return toSplitView(parsedSplit);
    }

    @Override
    public List<String> splitNames() {
        List<String> result = new ArrayList<>();

        if(_isManagerDestroyed){
            Logger.e("Manager has already been destroyed - no calls possible");
            return result;
        }

        List<ParsedSplit> parsedSplits = _splitFetcher.fetchAll();
        for (ParsedSplit split : parsedSplits) {
            result.add(split.feature());
        }
        return result;
    }

    @Override
    public void destroy() {
        _isManagerDestroyed = true;
    }

    private SplitView toSplitView(ParsedSplit parsedSplit) {
        SplitView splitView = new SplitView();
        splitView.name = parsedSplit.feature();
        splitView.trafficType = parsedSplit.trafficTypeName();
        splitView.killed = parsedSplit.killed();
        splitView.changeNumber = parsedSplit.changeNumber();
        splitView.configs = parsedSplit.configurations();

        Set<String> treatments = new HashSet<String>();
        for (ParsedCondition condition : parsedSplit.parsedConditions()) {
            for (Partition partition : condition.partitions()) {
                treatments.add(partition.treatment);
            }
        }
        treatments.add(parsedSplit.defaultTreatment());

        splitView.treatments = new ArrayList<String>(treatments);

        return splitView;
    }
}
