package io.split.android.client.service.mysegments;

import java.util.List;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.SplitChange;

// TODO: Replace current Split Fetcher interface with this one when synchronizer is developed
public interface MySegmentsFetcherV2 {
    List<MySegment> execute();
}
