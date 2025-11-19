package io.split.android.engine.experiments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Condition;
import io.split.android.client.dtos.Excluded;
import io.split.android.client.dtos.RuleBasedSegment;

public class RuleBasedSegmentParser implements Parser<RuleBasedSegment, ParsedRuleBasedSegment> {

    private final ParserCommons mParserCommons;

    public RuleBasedSegmentParser(@NonNull ParserCommons parserCommons) {
        mParserCommons = parserCommons;
    }

    @Nullable
    @Override
    public ParsedRuleBasedSegment parse(@NonNull RuleBasedSegment input, String matchingKey) {
        String name = input.getName();
        Excluded excluded = input.getExcluded();
        List<Condition> conditions = input.getConditions();
        List<ParsedCondition> parsedConditions = mParserCommons.getParsedConditions(
                matchingKey,
                conditions,
                "Dropping rule based segment name=" + name + " due to large number of conditions (" + conditions.size() + ")");

        if (parsedConditions == null) {
            parsedConditions = new ArrayList<>();
        }

        if (excluded == null) {
            excluded = Excluded.createEmpty();
        }

        return new ParsedRuleBasedSegment(name,
                excluded.getKeys(),
                excluded.getSegments(),
                parsedConditions,
                input.getTrafficTypeName(),
                input.getChangeNumber());
    }
}
