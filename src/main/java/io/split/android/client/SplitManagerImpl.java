package io.split.android.client;

import com.google.common.base.Strings;

import io.split.android.client.api.SplitView;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Split;
import io.split.android.client.utils.Logger;
import io.split.android.client.validators.SplitNameValidator;
import io.split.android.client.validators.Validator;
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


    public SplitManagerImpl(SplitFetcher splitFetcher) {
        _splitFetcher  = splitFetcher;
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

        if(_isManagerDestroyed){
            Logger.e("Manager has already been destroyed - no calls possible");
            return null;
        }

        Split split = new Split(featureName);
        Validator splitValidator = new SplitNameValidator("split");
        if (!split.isValid(splitValidator)) {
            return null;
        }

        ParsedSplit parsedSplit = _splitFetcher.fetch(featureName.trim());
        return parsedSplit == null ? null : toSplitView(parsedSplit);
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
