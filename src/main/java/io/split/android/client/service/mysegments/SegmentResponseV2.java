package io.split.android.client.service.mysegments;

import androidx.annotation.NonNull;

import java.util.Set;

public interface SegmentResponseV2 {

    @NonNull
    Set<String> getSegments();

    @NonNull
    Set<String> getLargeSegments();

    Long getSegmentsTill();

    Long getLargeSegmentsTill();
}
