package io.split.android.engine.matchers;

import java.util.Map;
import java.util.Set;

import io.split.android.client.Evaluator;

/**
 * Created by guillermo on 12/12/17.
 */

public class MySegmentsMatcher implements Matcher {

    private Set<String> _mySegments;
    private String _segmentName;

    public MySegmentsMatcher(Set<String> mySegments, String segmentName) {
        _mySegments = mySegments;
        _segmentName = segmentName;
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        return _mySegments.contains(_segmentName);
    }
}
