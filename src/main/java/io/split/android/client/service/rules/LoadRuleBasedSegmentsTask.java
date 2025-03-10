package io.split.android.client.service.rules;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.client.utils.logger.Logger;

public class LoadRuleBasedSegmentsTask implements SplitTask {

    private final RuleBasedSegmentStorage mRuleBasedSegmentStorage;

    public LoadRuleBasedSegmentsTask(@NonNull RuleBasedSegmentStorage ruleBasedSegmentStorage) {
        mRuleBasedSegmentStorage = checkNotNull(ruleBasedSegmentStorage);
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            mRuleBasedSegmentStorage.loadLocal();
            return SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_RULE_BASED_SEGMENTS);
        } catch (Exception e) {
            Logger.e("Error loading rule based segments: " + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(SplitTaskType.LOAD_LOCAL_RULE_BASED_SEGMENTS);
        }
    }
}
