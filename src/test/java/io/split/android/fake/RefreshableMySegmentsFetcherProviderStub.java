package io.split.android.fake;

import java.io.IOException;
import java.util.List;

import io.split.android.engine.segments.MySegments;
import io.split.android.engine.segments.RefreshableMySegmentsFetcherProvider;

public class RefreshableMySegmentsFetcherProviderStub implements RefreshableMySegmentsFetcherProvider {

    MySegments mMySegments;

    public RefreshableMySegmentsFetcherProviderStub(List<String> mySegments) {
        this.mMySegments = new MySegmentsStub(mySegments);
    }

    @Override
    public MySegments mySegments() {
        return mMySegments;
    }

    @Override
    public void close() throws IOException {
    }
}
