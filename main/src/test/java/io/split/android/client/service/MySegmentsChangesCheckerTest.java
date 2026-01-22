package io.split.android.client.service;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.split.android.client.service.synchronizer.MySegmentsChangeChecker;

public class MySegmentsChangesCheckerTest {

    MySegmentsChangeChecker mMySegmentsChangeChecker = new MySegmentsChangeChecker();

    @Test
    public void testChangesArrived() {
        List<String> old = Arrays.asList("s1", "s2", "s3");
        List<String> newSegments = Arrays.asList("s1");
        List<String> result = mMySegmentsChangeChecker.getChangedSegments(old, newSegments);

        Assert.assertFalse(result.isEmpty());
        // s2 and s3 were removed
        Set<String> changedSet = new HashSet<>(result);
        Assert.assertTrue(changedSet.contains("s2"));
        Assert.assertTrue(changedSet.contains("s3"));
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void testNewChangesArrived() {
        List<String> newSegments = Arrays.asList("s1", "s2", "s3");
        List<String> old = Arrays.asList("s1");
        List<String> result = mMySegmentsChangeChecker.getChangedSegments(old, newSegments);

        Assert.assertFalse(result.isEmpty());
        // s2 and s3 were added
        Set<String> changedSet = new HashSet<>(result);
        Assert.assertTrue(changedSet.contains("s2"));
        Assert.assertTrue(changedSet.contains("s3"));
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void testNoChangesArrived() {
        List<String> old = Arrays.asList("s1", "s2", "s3");
        List<String> newSegments = Arrays.asList("s1", "s2", "s3");
        List<String> result = mMySegmentsChangeChecker.getChangedSegments(old, newSegments);

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testNoChangesDifferentOrder() {
        List<String> old = Arrays.asList("s1", "s2", "s3");
        List<String> newSegments = Arrays.asList("s2", "s1", "s3");
        List<String> result = mMySegmentsChangeChecker.getChangedSegments(old, newSegments);

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testNoChangesDifferentOrderInverted() {
        List<String> newSegments = Arrays.asList("s1", "s2", "s3");
        List<String> old = Arrays.asList("s2", "s1", "s3");
        List<String> result = mMySegmentsChangeChecker.getChangedSegments(old, newSegments);

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testNoChangesArrivedEmpty() {
        List<String> newSegments = new ArrayList<>();
        List<String> old = new ArrayList<>();
        List<String> result = mMySegmentsChangeChecker.getChangedSegments(old, newSegments);

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testEmptyChangesArrived() {
        List<String> newSegments = new ArrayList<>();
        List<String> old = Arrays.asList("s1", "s2", "s3");
        List<String> result = mMySegmentsChangeChecker.getChangedSegments(old, newSegments);

        Assert.assertFalse(result.isEmpty());
        // s1, s2, s3 were all removed
        Set<String> changedSet = new HashSet<>(result);
        Assert.assertTrue(changedSet.contains("s1"));
        Assert.assertTrue(changedSet.contains("s2"));
        Assert.assertTrue(changedSet.contains("s3"));
        Assert.assertEquals(3, result.size());
    }
}
