package io.split.android.client.service.synchronizer.mysegments;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;

public interface MySegmentsSynchronizerFactory {

    MySegmentsSynchronizer getSynchronizer(MySegmentsTaskFactory mySegmentsTaskFactory, SplitEventsManager splitEventsManager);
}
