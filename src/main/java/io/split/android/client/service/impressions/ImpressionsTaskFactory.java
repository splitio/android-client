package io.split.android.client.service.impressions;

import java.util.List;

public interface ImpressionsTaskFactory {

    ImpressionsRecorderTask createImpressionsRecorderTask();

    SaveImpressionsCountTask createSaveImpressionsCountTask(List<ImpressionsCountPerFeature> count);

    ImpressionsCountRecorderTask createImpressionsCountRecorderTask();
}
