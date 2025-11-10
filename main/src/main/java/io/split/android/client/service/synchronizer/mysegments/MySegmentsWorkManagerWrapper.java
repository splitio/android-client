package io.split.android.client.service.synchronizer.mysegments;

import java.util.Set;

public interface MySegmentsWorkManagerWrapper {

    void scheduleMySegmentsWork(Set<String> keys);

    void removeWork();
}
