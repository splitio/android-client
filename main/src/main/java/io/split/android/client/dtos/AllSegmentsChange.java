package io.split.android.client.dtos;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AllSegmentsChange {

    @SerializedName("ms")
    private SegmentsChange mMySegmentsChange;

    @SerializedName("ls")
    private SegmentsChange mMyLargeSegmentsChange;

    public AllSegmentsChange() {

    }

    // TODO legacy endpoint support during development
    @Deprecated
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

    @VisibleForTesting
    public static AllSegmentsChange create(SegmentsChange mySegmentsChange, SegmentsChange myLargeSegmentsChange) {
        AllSegmentsChange allSegmentsChange = new AllSegmentsChange();
        allSegmentsChange.mMySegmentsChange = mySegmentsChange;
        allSegmentsChange.mMyLargeSegmentsChange = myLargeSegmentsChange;
        return allSegmentsChange;
    }
}
