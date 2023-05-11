package io.split.android.engine.experiments;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.Nullable;

import com.google.common.collect.Lists;

import java.util.List;

import io.split.android.client.dtos.Condition;
import io.split.android.client.dtos.Matcher;
import io.split.android.client.dtos.MatcherGroup;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.mysegments.EmptyMySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.utils.logger.Logger;
import io.split.android.engine.matchers.AllKeysMatcher;
import io.split.android.engine.matchers.AttributeMatcher;
import io.split.android.engine.matchers.BetweenMatcher;
import io.split.android.engine.matchers.BooleanMatcher;
import io.split.android.engine.matchers.CombiningMatcher;
import io.split.android.engine.matchers.DependencyMatcher;
import io.split.android.engine.matchers.EqualToMatcher;
import io.split.android.engine.matchers.GreaterThanOrEqualToMatcher;
import io.split.android.engine.matchers.LessThanOrEqualToMatcher;
import io.split.android.engine.matchers.MySegmentsMatcher;
import io.split.android.engine.matchers.collections.ContainsAllOfSetMatcher;
import io.split.android.engine.matchers.collections.ContainsAnyOfSetMatcher;
import io.split.android.engine.matchers.collections.EqualToSetMatcher;
import io.split.android.engine.matchers.collections.PartOfSetMatcher;
import io.split.android.engine.matchers.strings.ContainsAnyOfMatcher;
import io.split.android.engine.matchers.strings.EndsWithAnyOfMatcher;
import io.split.android.engine.matchers.strings.RegularExpressionMatcher;
import io.split.android.engine.matchers.strings.StartsWithAnyOfMatcher;
import io.split.android.engine.matchers.strings.WhitelistMatcher;

public class SplitParser {

    public static final int CONDITIONS_UPPER_LIMIT = 50;

    private final MySegmentsStorageContainer mMySegmentsStorageContainer;

    public static SplitParser get(MySegmentsStorageContainer mySegmentsStorageContainer) {
        return new SplitParser(mySegmentsStorageContainer);
    }

    public SplitParser(MySegmentsStorageContainer mySegmentsStorageContainer) {
        mMySegmentsStorageContainer = checkNotNull(mySegmentsStorageContainer);
    }

    @Nullable
    public ParsedSplit parse(@Nullable Split split) {
        return parse(split, null);
    }

    @Nullable
    public ParsedSplit parse(@Nullable Split split, @Nullable String matchingKey) {
        try {
            return parseWithoutExceptionHandling(split, matchingKey);
        } catch (Throwable t) {
            Logger.e(t, "Could not parse feature flag: %s", split);
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

        if (split.conditions.size() > CONDITIONS_UPPER_LIMIT) {
            Logger.w("Dropping feature flag name=%s due to large number of conditions(%d)",
                    split.name, split.conditions.size());
            return null;
        }

        List<ParsedCondition> parsedConditionList = Lists.newArrayList();

        for (Condition condition : split.conditions) {
            List<Partition> partitions = condition.partitions;
            CombiningMatcher matcher = toMatcher(condition.matcherGroup, matchingKey);
            parsedConditionList.add(new ParsedCondition(condition.conditionType, matcher, partitions, condition.label));
        }

        return new ParsedSplit(split.name, split.seed, split.killed, split.defaultTreatment, parsedConditionList, split.trafficTypeName, split.changeNumber, split.trafficAllocation, split.trafficAllocationSeed, split.algo, split.configurations);
    }

    private CombiningMatcher toMatcher(MatcherGroup matcherGroup, String matchingKey) {
        List<Matcher> matchers = matcherGroup.matchers;
        checkArgument(!matchers.isEmpty());

        List<AttributeMatcher> toCombine = Lists.newArrayList();

        for (Matcher matcher : matchers) {
            toCombine.add(toMatcher(matcher, matchingKey));
        }

        return new CombiningMatcher(matcherGroup.combiner, toCombine);
    }

    private AttributeMatcher toMatcher(Matcher matcher, String matchingKey) {
        io.split.android.engine.matchers.Matcher delegate;
        switch (matcher.matcherType) {
            case ALL_KEYS:
                delegate = new AllKeysMatcher();
                break;
            case IN_SEGMENT:
                checkNotNull(matcher.userDefinedSegmentMatcherData);
                delegate = new MySegmentsMatcher((matchingKey != null) ? mMySegmentsStorageContainer.getStorageForKey(matchingKey) : new EmptyMySegmentsStorage(), matcher.userDefinedSegmentMatcherData.segmentName);
                break;
            case WHITELIST:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new WhitelistMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case EQUAL_TO:
                checkNotNull(matcher.unaryNumericMatcherData);
                delegate = new EqualToMatcher(matcher.unaryNumericMatcherData.value, matcher.unaryNumericMatcherData.dataType);
                break;
            case GREATER_THAN_OR_EQUAL_TO:
                checkNotNull(matcher.unaryNumericMatcherData);
                delegate = new GreaterThanOrEqualToMatcher(matcher.unaryNumericMatcherData.value, matcher.unaryNumericMatcherData.dataType);
                break;
            case LESS_THAN_OR_EQUAL_TO:
                checkNotNull(matcher.unaryNumericMatcherData);
                delegate = new LessThanOrEqualToMatcher(matcher.unaryNumericMatcherData.value, matcher.unaryNumericMatcherData.dataType);
                break;
            case BETWEEN:
                checkNotNull(matcher.betweenMatcherData);
                delegate = new BetweenMatcher(matcher.betweenMatcherData.start, matcher.betweenMatcherData.end, matcher.betweenMatcherData.dataType);
                break;
            case EQUAL_TO_SET:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new EqualToSetMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case PART_OF_SET:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new PartOfSetMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case CONTAINS_ALL_OF_SET:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new ContainsAllOfSetMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case CONTAINS_ANY_OF_SET:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new ContainsAnyOfSetMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case STARTS_WITH:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new StartsWithAnyOfMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case ENDS_WITH:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new EndsWithAnyOfMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case CONTAINS_STRING:
                checkNotNull(matcher.whitelistMatcherData);
                delegate = new ContainsAnyOfMatcher(matcher.whitelistMatcherData.whitelist);
                break;
            case MATCHES_STRING:
                checkNotNull(matcher.stringMatcherData);
                delegate = new RegularExpressionMatcher(matcher.stringMatcherData);
                break;
            case IN_SPLIT_TREATMENT:
                checkNotNull(matcher.dependencyMatcherData,
                        "MatcherType is " + matcher.matcherType
                                + ". matcher.dependencyMatcherData() MUST NOT BE null");
                delegate = new DependencyMatcher(matcher.dependencyMatcherData.split, matcher.dependencyMatcherData.treatments);
                break;
            case EQUAL_TO_BOOLEAN:
                checkNotNull(matcher.booleanMatcherData,
                        "MatcherType is " + matcher.matcherType
                                + ". matcher.booleanMatcherData() MUST NOT BE null");
                delegate = new BooleanMatcher(matcher.booleanMatcherData);
                break;
            default:
                throw new IllegalArgumentException("Unknown matcher type: " + matcher.matcherType);
        }

        checkNotNull(delegate, "We were not able to create a matcher for: " + matcher.matcherType);

        String attribute = null;
        if (matcher.keySelector != null && matcher.keySelector.attribute != null) {
            attribute = matcher.keySelector.attribute;
        }

        boolean negate = matcher.negate;


        return new AttributeMatcher(attribute, delegate, negate);
    }
}
