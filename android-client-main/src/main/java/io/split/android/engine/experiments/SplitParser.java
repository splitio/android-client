package io.split.android.engine.experiments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.utils.logger.Logger;

public class SplitParser implements Parser<Split, ParsedSplit> {

    private final ParserCommons mParserCommons;

    public SplitParser(ParserCommons parserCommons) {
        mParserCommons = checkNotNull(parserCommons);
    }

    @Nullable
    public ParsedSplit parse(@Nullable Split split) {
        return parse(split, null);
    }

    @Override
    @Nullable
    public ParsedSplit parse(@Nullable Split split, @Nullable String matchingKey) {
        try {
            return parseWithoutExceptionHandling(split, matchingKey);
        } catch (Throwable t) {
            Logger.e(t, "Could not parse feature flag: %s", (split != null) ? split.name : "null");
            return null;
        }
    }

    private ParsedSplit parseWithoutExceptionHandling(Split split, String matchingKey) {
        if (split == null) {
            return null;
        }

        if (split.status != Status.ACTIVE) {
            return null;
        }

        List<ParsedCondition> parsedConditionList = mParserCommons.getParsedConditions(matchingKey, split.conditions,
                "Dropping feature flag name=" + split.name + " due to large number of conditions (" + split.conditions.size() + ")");
        if (parsedConditionList == null) {
            return null;
        }

        return new ParsedSplit(split.name,
                split.seed,
                split.killed,
                split.defaultTreatment,
                parsedConditionList,
                split.trafficTypeName,
                split.changeNumber,
                split.trafficAllocation,
                split.trafficAllocationSeed,
                split.algo,
                split.configurations,
                split.sets,
                split.impressionsDisabled,
                new ArrayList<>(split.getPrerequisites()));
    }
}
