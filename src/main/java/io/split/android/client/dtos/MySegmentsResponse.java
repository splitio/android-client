package io.split.android.client.dtos;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MySegmentsResponse implements SegmentResponse {

    @SerializedName("mySegments")
    private List<MySegment> mySegments;

    @SerializedName("till")
    private Long till;

    @NonNull
    @Override
    public List<String> getSegments() {
        return mySegments == null ? Collections.emptyList() :
                mapToNames(mySegments);
    }

    @Override
    public long getTill() {
        return till == null ? -1 : till;
    }

    @VisibleForTesting
    public static MySegmentsResponse create(List<MySegment> mySegments, long till) {
        MySegmentsResponse mySegmentsResponse = new MySegmentsResponse();
        mySegmentsResponse.mySegments = mySegments;
        mySegmentsResponse.till = till;
        return mySegmentsResponse;
    }

    private static List<String> mapToNames(List<MySegment> mySegments) {
        List<String> names = new ArrayList<>();
        for (MySegment segment : mySegments) {
            names.add(segment.name);
        }

        return names;
    }
}
