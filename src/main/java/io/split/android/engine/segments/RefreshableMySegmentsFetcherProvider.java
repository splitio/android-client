package io.split.android.engine.segments;

import java.io.Closeable;

public interface RefreshableMySegmentsFetcherProvider extends Closeable {
    MySegments mySegments();
}
