package io.split.android.client.service.impressions;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.service.impressions.unique.SaveUniqueImpressionsTask;
import io.split.android.client.service.impressions.unique.UniqueKeysRecorderTask;

public interface ImpressionsTaskFactory {

    ImpressionsRecorderTask createImpressionsRecorderTask();

    SaveImpressionsCountTask createSaveImpressionsCountTask(List<ImpressionsCountPerFeature> count);

    ImpressionsCountRecorderTask createImpressionsCountRecorderTask();

    SaveUniqueImpressionsTask createSaveUniqueImpressionsTask(Map<String, Set<String>> uniqueImpressions);

    UniqueKeysRecorderTask createUniqueImpressionsRecorderTask();
}
