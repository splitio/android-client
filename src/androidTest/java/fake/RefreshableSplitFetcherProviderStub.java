package fake;

import io.split.android.engine.experiments.RefreshableSplitFetcher;
import io.split.android.engine.experiments.RefreshableSplitFetcherProvider;

public class RefreshableSplitFetcherProviderStub implements RefreshableSplitFetcherProvider {
    @Override
    public RefreshableSplitFetcher getFetcher() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }
}
