package io.split.android.client.storage.splits;

import io.split.android.client.dtos.SplitChange;

// TODO: Replace current Split Fetcher interface with this one when synchronizer is developed
public interface SplitFetcherV2 {
    SplitChange execute(long since);
}
