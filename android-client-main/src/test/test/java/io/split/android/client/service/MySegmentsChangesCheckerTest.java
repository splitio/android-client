package io.split.android.client.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.split.android.client.service.synchronizer.MySegmentsChangeChecker;

public class MySegmentsChangesCheckerTest {

    MySegmentsChangeChecker mMySegmentsChangeChecker = new MySegmentsChangeChecker();

    @Test
    public void testChangesArrived() {

        List<String> old = Arrays.asList("s1", "s2", "s3");
        List<String> newSegments = Arrays.asList("s1");
        boolean result = mMySegmentsChangeChecker.mySegmentsHaveChanged(old, newSegments);

        Assert.assertTrue(result);
    }

    @Test
    public void testNewChangesArrived() {

        List<String> newSegments = Arrays.asList("s1", "s2", "s3");
        List<String> old = Arrays.asList("s1");
        boolean result = mMySegmentsChangeChecker.mySegmentsHaveChanged(old, newSegments);

        Assert.assertTrue(result);
    }

    @Test
    public void testNoChangesArrived() {

        List<String> old = Arrays.asList("s1", "s2", "s3");
        List<String> newSegments = Arrays.asList("s1", "s2", "s3");
        boolean result = mMySegmentsChangeChecker.mySegmentsHaveChanged(old, newSegments);

        Assert.assertFalse(result);
    }

    @Test
    public void testNoChangesDifferentOrder() {

        List<String> old = Arrays.asList("s1", "s2", "s3");
        List<String> newSegments = Arrays.asList("s2", "s1", "s3");
        boolean result = mMySegmentsChangeChecker.mySegmentsHaveChanged(old, newSegments);

        Assert.assertFalse(result);
    }

    @Test
    public void testNoChangesDifferentOrderInverted() {

        List<String> newSegments = Arrays.asList("s1", "s2", "s3");
        List<String> old = Arrays.asList("s2", "s1", "s3");
        boolean result = mMySegmentsChangeChecker.mySegmentsHaveChanged(old, newSegments);

        Assert.assertFalse(result);
    }

    @Test
    public void testNoChangesArrivedEmpty() {

        List<String> newSegments = new ArrayList<>();
        List<String> old = new ArrayList<>();
        boolean result = mMySegmentsChangeChecker.mySegmentsHaveChanged(old, newSegments);

        Assert.assertFalse(result);
    }

    @Test
    public void testEmptyChangesArrived() {

        List<String> newSegments = new ArrayList<>();
        List<String> old = Arrays.asList("s1", "s2", "s3");
        boolean result = mMySegmentsChangeChecker.mySegmentsHaveChanged(old, newSegments);

        Assert.assertTrue(result);
    }
}
