package io.split.android.client.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SdkVersionProviderTest {

    @Test
    public void getSdkVersionStartsWithAndroidPrefix() {
        assertTrue(SdkVersionProvider.getSdkVersion().startsWith("Android-"));
    }

    @Test
    public void getSdkVersionContainsNonEmptyVersionAfterPrefix() {
        String version = SdkVersionProvider.getSdkVersion();
        String suffix = version.substring("Android-".length());
        assertFalse(suffix.isEmpty());
    }
}
