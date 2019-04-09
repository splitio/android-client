package io.split.android.fake;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.split.android.engine.segments.MySegments;

public class MySegmentsStub implements MySegments{

    Set<String> mMySegments;

    public MySegmentsStub(List<String> mySegments) {
        this.mMySegments = new HashSet<>(mySegments);
    }

    @Override
    public boolean contains(String segmentName) {
        return mMySegments.contains(segmentName);
    }

    @Override
    public void forceRefresh() {
    }
}
