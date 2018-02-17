package io.split.android.engine.segments;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.MySegment;

/**
 * Created by fernandomartin on 2/17/18.
 */

public class StaticMySegments implements MySegments {

    private final List<MySegment> mySegments;

    public StaticMySegments(String... mySegments) {
        this(Lists.newArrayList(mySegments));
    }

    public StaticMySegments(List<String> mySegmentNames) {
        this.mySegments = new ArrayList<>();
        for(String mySegmentName : mySegmentNames) {
            MySegment toBeAdded = new MySegment();
            toBeAdded.name = mySegmentName;
            mySegments.add(toBeAdded);
        }
    }

    @Override
    public boolean contains(String segmentName) {
        for(MySegment mySegment : mySegments) {
            if (mySegment.name.equals(segmentName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void forceRefresh() {

    }
}
