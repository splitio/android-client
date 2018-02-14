package io.split.android.engine.segments;

import java.util.List;

import io.split.android.client.dtos.MySegment;

/**
 * Created by guillermo on 11/29/17.
 */

public interface MySegmentsFetcher {

    List<MySegment> fetch(String matchingKey);
}
