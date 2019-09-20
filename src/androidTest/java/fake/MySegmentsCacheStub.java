package fake;

import java.util.List;

import io.split.android.client.cache.IMySegmentsCache;
import io.split.android.client.dtos.MySegment;

public class MySegmentsCacheStub implements IMySegmentsCache {
    @Override
    public void setMySegments(String key, List<MySegment> mySegments) {
    }

    @Override
    public List<MySegment> getMySegments(String key) {
        return null;
    }

    @Override
    public void deleteMySegments(String key) {
    }

    @Override
    public void saveToDisk() {
    }
}
