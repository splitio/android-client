package io.split.android.client.service.http.mysegments;

import java.util.List;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.service.http.HttpFetcher;

public interface MySegmentsFetcherFactory {

    HttpFetcher<List<MySegment>> getFetcher(String userKey);
}
