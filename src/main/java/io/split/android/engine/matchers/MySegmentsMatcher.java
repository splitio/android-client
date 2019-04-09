package io.split.android.engine.matchers;

import java.util.Map;

import io.split.android.client.Evaluator;
import io.split.android.client.SplitClientImpl;
import io.split.android.engine.segments.MySegments;

/**
 * Created by guillermo on 12/12/17.
 */

public class MySegmentsMatcher implements Matcher {

    private MySegments _mySegments;
    private String _segmentName;

    public MySegmentsMatcher(MySegments mySegments, String segmentName) {
        _mySegments = mySegments;
        _segmentName = segmentName;
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        return _mySegments.contains(_segmentName);
    }
}
