package io.split.android.client.shared;

import java.util.Set;

import io.split.android.client.SplitClient;
import io.split.android.client.api.Key;

public interface SplitClientContainer {

    SplitClient getClient(Key key);

    void remove(String key);

    Set<SplitClient> getAll();
}
