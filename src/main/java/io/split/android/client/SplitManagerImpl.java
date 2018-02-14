package io.split.android.client;

import io.split.android.client.api.SplitView;
import io.split.android.client.dtos.Partition;
import io.split.android.engine.experiments.ParsedCondition;
import io.split.android.engine.experiments.ParsedSplit;
import io.split.android.engine.experiments.SplitFetcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SplitManagerImpl implements SplitManager {

    private final SplitFetcher _splitFetcher;


    public SplitManagerImpl(SplitFetcher splitFetcher) {
        _splitFetcher  = splitFetcher;
    }

    @Override
    public List<SplitView> splits() {
        List<SplitView> result = new ArrayList<>();
        List<ParsedSplit> parsedSplits = _splitFetcher.fetchAll();
        for (ParsedSplit split : parsedSplits) {
            result.add(toSplitView(split));
        }
        return result;
    }

    @Override
    public SplitView split(String featureName) {
        ParsedSplit parsedSplit = _splitFetcher.fetch(featureName);
        return parsedSplit == null ? null : toSplitView(parsedSplit);
    }

    @Override
    public List<String> splitNames() {
        List<String> result = new ArrayList<>();
        List<ParsedSplit> parsedSplits = _splitFetcher.fetchAll();
        for (ParsedSplit split : parsedSplits) {
            result.add(split.feature());
        }
        return result;
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
