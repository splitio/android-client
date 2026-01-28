package io.split.android.engine.experiments;

import java.util.Collections;
import java.util.List;

import io.split.android.client.TreatmentLabels;
import io.split.android.client.dtos.ConditionType;
import io.split.android.client.dtos.MatcherCombiner;
import io.split.android.client.dtos.Partition;
import io.split.android.engine.matchers.AllKeysMatcher;
import io.split.android.engine.matchers.AttributeMatcher;
import io.split.android.engine.matchers.CombiningMatcher;

class DefaultConditionsProvider {

    List<ParsedCondition> getDefaultConditions() {
        Partition partition = new Partition();
        partition.size = 100;
        partition.treatment = "control";
        ParsedCondition condition = new ParsedCondition(
                ConditionType.WHITELIST,
                new CombiningMatcher(MatcherCombiner.AND, Collections.singletonList(AttributeMatcher.vanilla(new AllKeysMatcher()))),
                Collections.singletonList(partition),
                TreatmentLabels.UNSUPPORTED_MATCHER_TYPE);

        return Collections.singletonList(condition);
    }
}
