package io.split.android.client.storage.cache;

import io.split.android.client.dtos.SplitChange;

public interface NewSplitFetcher {
    SplitChange execute(long since);
}
