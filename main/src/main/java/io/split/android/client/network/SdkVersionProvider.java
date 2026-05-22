package io.split.android.client.network;

import io.split.android.client.main.BuildConfig;

public class SdkVersionProvider {

    public static String getSdkVersion() {
        return "Android-" + BuildConfig.SPLIT_VERSION_NAME;
    }
}
