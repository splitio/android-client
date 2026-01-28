package io.split.android.client.service.impressions.strategy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Test;

import io.split.android.client.impressions.Impression;

public class UtilsTest {

    @Test
    public void hasPropertiesReturnsTrueWhenPropertiesAreNotNUll() {
        assertTrue(Utils.hasProperties(getImpression("{\"key\":\"value\"}")));
    }

    @Test
    public void hasPropertiesReturnsFalseWhenPropertiesAreNull() {
        assertFalse(Utils.hasProperties(getImpression(null)));
    }

    @Test
    public void hasPropertiesReturnsFalseWhenPropertiesIsEmpty() {
        assertFalse(Utils.hasProperties(getImpression("")));
    }

    @NonNull
    private static Impression getImpression(String props) {
        return new Impression(
                "key",
                "bkey",
                "flag",
                "on",
                System.currentTimeMillis(),
                "default rule",
                999L,
                null,
                props);
    }
}
