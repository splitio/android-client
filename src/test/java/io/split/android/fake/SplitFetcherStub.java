package io.split.android.fake;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Split;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.engine.experiments.ParsedSplit;
import io.split.android.engine.experiments.SplitFetcher;
import io.split.android.engine.experiments.SplitParser;

@SuppressWarnings("ConstantConditions")
public class SplitFetcherStub implements SplitFetcher {

    Map<String, ParsedSplit> mSplits;
    SplitParser mParser;

    public SplitFetcherStub(List<Split> splits, MySegmentsStorage mySegmentsStorage){
        mParser = new SplitParser(mySegmentsStorage);
        mSplits = new HashMap<>();
        for (Split split : splits) {
           mSplits.put(split.name, mParser.parse(split));
        }
    }

    @Override
    public ParsedSplit fetch(String splitName) {
        return mSplits.get(splitName);
    }

    @Override
    public List<ParsedSplit> fetchAll() {
        return (List<ParsedSplit>) mSplits.values();
    }

    @Override
    public void forceRefresh() {
    }

    private ParsedSplit parseSplit(Split split) {
        return mParser.parse(split);
    }
}