package io.split.android.engine.matchers;

import java.util.Map;
import java.util.Set;

import io.split.android.client.Evaluator;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;

/**
 * Created by guillermo on 12/12/17.
 */

public class MySegmentsMatcher implements Matcher {

    private final String mSegmentName;
    private final MySegmentsStorageContainer mMySegmentsStorageContainer;

    public MySegmentsMatcher(MySegmentsStorageContainer mySegmentsStorageContainer, String segmentName) {
        mMySegmentsStorageContainer = mySegmentsStorageContainer;
        mSegmentName = segmentName;
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        return mMySegmentsStorageContainer.getStorageForKey(bucketingKey).getAll().contains(mSegmentName);
    }
}
