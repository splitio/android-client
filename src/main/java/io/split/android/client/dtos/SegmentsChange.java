package io.split.android.client.dtos;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SegmentsChange {
    @SerializedName("k")
    private Set<Segment> mSegments;

    @SerializedName("cn")
    private Long mChangeNumber;

    public SegmentsChange(Set<Segment> segments, Long changeNumber) {
        mSegments = segments;
        mChangeNumber = changeNumber;
    }

    public Set<Segment> getSegments() {
        return mSegments == null ? Collections.emptySet() : mSegments;
    }

    @Nullable
    public Long getChangeNumber() {
        return mChangeNumber;
    }

    public List<String> getNames() {
        Set<Segment> segments = new HashSet<>(getSegments());
        List<String> names = new ArrayList<>(segments.size());
        for (Segment segment : segments) {
            names.add(segment.getName());
        }
        return names;
    }

    public static SegmentsChange createEmpty() {
        return new SegmentsChange(Collections.emptySet(), null);
    }

    public static SegmentsChange create(Set<String> segments, @Nullable Long changeNumber) {
        Set<Segment> segmentSet = new HashSet<>();
        for (String segment : segments) {
            Segment segmentObj = new Segment();
            segmentObj.setName(segment);
            segmentSet.add(segmentObj);
        }
        return new SegmentsChange(segmentSet, changeNumber);
    }
}
