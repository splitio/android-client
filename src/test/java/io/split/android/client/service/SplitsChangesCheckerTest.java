package io.split.android.client.service;

import org.junit.Assert;
import org.junit.Test;

import io.split.android.client.service.synchronizer.SplitsChangeChecker;

public class SplitsChangesCheckerTest {
    SplitsChangeChecker mSplitsChangesChecker = new SplitsChangeChecker();

    @Test
    public void testSplitsChangesArrived() {
        boolean result = mSplitsChangesChecker.changeNumberIsNewer( 100,  101);

        Assert.assertTrue(result);
    }

    @Test
    public void testSplitsNoChangesMinorChangeNumber() {
        boolean result = mSplitsChangesChecker.changeNumberIsNewer( 101,  100);

        Assert.assertFalse(result);
    }

    @Test
    public void testSplitsNoChangesEqualChangeNumber() {
        boolean result = mSplitsChangesChecker.changeNumberIsNewer( 100,  100);

        Assert.assertFalse(result);
    }
}
