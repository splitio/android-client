package io.split.android.engine.experiments;

import java.io.Closeable;

public interface RefreshableSplitFetcherProvider extends Closeable {
    RefreshableSplitFetcher getFetcher();

    @Override
    void close();

    void pause();

    void resume();
}
