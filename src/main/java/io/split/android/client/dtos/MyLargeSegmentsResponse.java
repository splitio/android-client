package io.split.android.client.dtos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class MyLargeSegmentsResponse {

    @SerializedName("myLargeSegments")
    private List<String> myLargeSegments;

    @SerializedName("till")
    private Long till;

    @NonNull
    public List<String> getMyLargeSegments() {
        return myLargeSegments == null ? new ArrayList<>() :
                myLargeSegments;
    }

    @Nullable
    public Long getTill() {
        return till;
    }
}
