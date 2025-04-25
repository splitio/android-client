package io.split.android.client.service.impressions.strategy;

import androidx.annotation.NonNull;

import io.split.android.client.impressions.Impression;

class Utils {

    static boolean hasProperties(@NonNull Impression impression) {
        if (impression == null) { // safety check
            return false;
        }

        String properties = impression.properties();
        return properties != null && !properties.isEmpty();
    }
}
