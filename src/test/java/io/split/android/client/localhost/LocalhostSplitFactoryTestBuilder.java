package io.split.android.client.localhost;

import androidx.annotation.NonNull;

import io.split.android.client.shared.SplitClientContainer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.engine.experiments.SplitParser;

class LocalhostSplitFactoryTestBuilder {

    static LocalhostSplitFactory getFactory(@NonNull SplitsStorage splitsStorage,
                                            @NonNull SplitParser splitParser,
                                            @NonNull String defaultKey,
                                            @NonNull LocalhostSynchronizer synchronizer,
                                            @NonNull SplitClientContainer clientContainer) {
        return new LocalhostSplitFactory(splitsStorage, splitParser, defaultKey, synchronizer, clientContainer);
    }
}
