package io.split.android.client.storage.common;

import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.storage.rbs.PersistentRuleBasedSegmentStorage;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorageImpl;
import io.split.android.engine.experiments.ParserCommons;
import io.split.android.engine.experiments.RuleBasedSegmentParser;

class RuleBasedSegmentStorageInitializer {

    static Result initialize(MySegmentsStorageContainer mySegmentsStorageContainer, MySegmentsStorageContainer myLargeSegmentsStorageContainer, PersistentRuleBasedSegmentStorage persistentRuleBasedSegmentStorage) {
        ParserCommons parserCommons = new ParserCommons(
                mySegmentsStorageContainer,
                myLargeSegmentsStorageContainer);
        RuleBasedSegmentStorage ruleBasedSegmentStorage = new RuleBasedSegmentStorageImpl(persistentRuleBasedSegmentStorage, new RuleBasedSegmentParser(parserCommons));
        parserCommons.setRuleBasedSegmentStorage(ruleBasedSegmentStorage);
        return new Result(ruleBasedSegmentStorage, parserCommons);
    }

    static class Result {
        final RuleBasedSegmentStorage mRuleBasedSegmentStorage;
        final ParserCommons mParserCommons;

        Result(RuleBasedSegmentStorage ruleBasedSegmentStorage, ParserCommons parserCommons) {
            mRuleBasedSegmentStorage = ruleBasedSegmentStorage;
            mParserCommons = parserCommons;
            mParserCommons.setRuleBasedSegmentStorage(mRuleBasedSegmentStorage);
        }

        public ParserCommons getParserCommons() {
            return mParserCommons;
        }

        public RuleBasedSegmentStorage getRuleBasedSegmentStorage() {
            return mRuleBasedSegmentStorage;
        }
    }
}
