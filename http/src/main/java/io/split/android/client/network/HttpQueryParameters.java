package io.split.android.client.network;

import androidx.annotation.NonNull;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class HttpQueryParameters {

    private final Set<Map.Entry<String, String>> mParams = new LinkedHashSet<>();

    @NonNull
    public HttpQueryParameters add(@NonNull String key, @NonNull String value) {
        mParams.add(new AbstractMap.SimpleEntry<>(key, value));
        return this;
    }

    @NonNull
    public Set<Map.Entry<String, String>> entries() {
        return Collections.unmodifiableSet(mParams);
    }

    public boolean isEmpty() {
        return mParams.isEmpty();
    }
}
