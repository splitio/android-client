package io.split.android.client.dtos;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AllSegmentsChange {

    @SerializedName("ms")
    private SegmentsChange mMySegmentsChange;

    @SerializedName("ls")
    private SegmentsChange mMyLargeSegmentsChange;

    // TODO legacy endpoint support during development
    public AllSegmentsChange(List<String> mySegments) {
        Set<Segment> segments = new HashSet<>();
        for (String name : mySegments) {
            Segment segment = new Segment();
            segment.setName(name);
            segments.add(segment);
        }
        mMySegmentsChange = new SegmentsChange(segments, null);
    }

    @Nullable
    public SegmentsChange getSegmentsChange() {
        return mMySegmentsChange;
    }

    @Nullable
    public SegmentsChange getLargeSegmentsChange() {
        return mMyLargeSegmentsChange;
    }
}
