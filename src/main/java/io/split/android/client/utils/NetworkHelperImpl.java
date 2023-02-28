package io.split.android.client.utils;

import java.net.URI;

public class NetworkHelperImpl implements NetworkHelper {
    @Override
    public boolean isReachable(URI target) {
        return true;
    }
}
