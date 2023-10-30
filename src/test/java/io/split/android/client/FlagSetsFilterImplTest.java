package io.split.android.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class FlagSetsFilterImplTest {


    @Test
    public void intersectReturnsTrueWhenShouldFilterIsFalse() {
        Set<String> flagSets = new HashSet<>();
        FlagSetsFilterImpl filter = new FlagSetsFilterImpl(flagSets);
        assertTrue(filter.intersect(new HashSet<>()));
    }

    @Test
    public void intersectReturnsTrueWhenSetsIsNull() {
        Set<String> flagSets = new HashSet<>();
        flagSets.add("test");
        FlagSetsFilterImpl filter = new FlagSetsFilterImpl(flagSets);
        assertFalse(filter.intersect((Set<String>) null));
    }

    @Test
    public void intersectReturnsTrueWhenSetIsContained() {
        Set<String> flagSets = new HashSet<>();
        flagSets.add("test");
        FlagSetsFilterImpl filter = new FlagSetsFilterImpl(flagSets);
        Set<String> testSet = new HashSet<>();
        testSet.add("test");
        assertTrue(filter.intersect(testSet));
    }

    @Test
    public void intersectReturnsFalseWhenSetIsNotContained() {
        Set<String> flagSets = new HashSet<>();
        flagSets.add("test");
        FlagSetsFilterImpl filter = new FlagSetsFilterImpl(flagSets);
        Set<String> testSet = new HashSet<>();
        testSet.add("other");
        assertFalse(filter.intersect(testSet));
    }

    @Test
    public void intersectReturnsTrueWhenStringSetIsNull() {
        Set<String> flagSets = new HashSet<>();
        flagSets.add("test");
        FlagSetsFilterImpl filter = new FlagSetsFilterImpl(flagSets);
        assertFalse(filter.intersect((String) null));
    }

    @Test
    public void intersectReturnsTrueWhenStringSetIsContained() {
        Set<String> flagSets = new HashSet<>();
        flagSets.add("test");
        FlagSetsFilterImpl filter = new FlagSetsFilterImpl(flagSets);
        assertTrue(filter.intersect("test"));
    }

    @Test
    public void intersectReturnsFalseWhenStringSetIsNotContained() {
        Set<String> flagSets = new HashSet<>();
        flagSets.add("test");
        FlagSetsFilterImpl filter = new FlagSetsFilterImpl(flagSets);
        assertFalse(filter.intersect("other"));
    }

}
