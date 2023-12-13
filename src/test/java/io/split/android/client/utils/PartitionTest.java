package io.split.android.client.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PartitionTest {

    @Test
    public void partitionWithEvenNumberOfPieces() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<List<Integer>> result = Utils.partition(list, 2);

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
        assertEquals(Arrays.asList(5, 6), result.get(2));
    }

    @Test
    public void partitionWithUnevenNumberOfPieces() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<List<Integer>> result = Utils.partition(list, 3);

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(4, 5), result.get(1));
    }

    @Test
    public void partitionListWithSingleElement() {
        List<Integer> list = Collections.singletonList(1);
        List<List<Integer>> result = Utils.partition(list, 1);

        assertEquals(1, result.size());
        assertEquals(Collections.singletonList(1), result.get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void partitionWithInvalidSizeAsInputThrows() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        Utils.partition(list, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void partitionWithNullListThrows() {
        Utils.partition(null, 1);
    }

    @Test
    public void partitionWithEmptyList() {
        List<Integer> list = Collections.emptyList();
        List<List<Integer>> result = Utils.partition(list, 1);

        assertTrue(result.isEmpty());
    }
}
