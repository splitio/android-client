package io.split.android.client.dtos;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class MyLargeSegmentsResponse implements SegmentResponse {

    @SerializedName("myLargeSegments")
    private List<String> myLargeSegments;

    @SerializedName("till")
    private Long till;

    @Override
    @NonNull
    public List<String> getSegments() {
        return myLargeSegments == null ? Collections.emptyList() :
                myLargeSegments;
    }

    @Override
    public long getTill() {
        return till == null ? -1 : till;
    }
}
