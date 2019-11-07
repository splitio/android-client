package io.split.android.client.utils;

import java.net.URI;

public class NetworkHelper {
    public boolean isReachable(URI target) {
        return Utils.isReachable(target);
    }
}
