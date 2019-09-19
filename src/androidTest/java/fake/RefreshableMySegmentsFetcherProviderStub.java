package fake;

import java.io.IOException;

import io.split.android.engine.segments.MySegments;
import io.split.android.engine.segments.RefreshableMySegmentsFetcherProvider;

public class RefreshableMySegmentsFetcherProviderStub implements RefreshableMySegmentsFetcherProvider {
    @Override
    public MySegments mySegments() {
        return null;
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void close() throws IOException {

    }
}
