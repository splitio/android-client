package io.split.android.integration.storage;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class Helper {

    @NonNull
    static <T> Set<T> asSet(T... elements) {
        if (elements.length == 0) {
            return Collections.emptySet();
        }
        Set<T> result = new HashSet<>();
        Collections.addAll(result, elements);

        return result;
    }

    public static final String JSON_SPLIT_WITH_TRAFFIC_TYPE_TEMPLATE = "{\"name\":\"%s\", \"changeNumber\": %d, \"trafficTypeName\":\"%s\", \"sets\":[\"%s\"]}";
}
