package io.split.android.client.dtos;

import androidx.annotation.NonNull;

import java.util.List;

public interface SegmentResponse {
    @NonNull
    List<String> getSegments();

    long getTill();
}
