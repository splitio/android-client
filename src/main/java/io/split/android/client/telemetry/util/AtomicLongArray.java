package io.split.android.client.telemetry.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class AtomicLongArray {
    private final AtomicLong[] array;
    private static final int MAX_LENGTH = 23;

    public AtomicLongArray(int size) {
        if (size <= 0) {
            size = MAX_LENGTH;
        }

        array = new AtomicLong[size];
        int bound = array.length;
        for (int x = 0; x < bound; x++) {
            array[x] = new AtomicLong();
        }
    }

    public void increment(int index) {
        if (index < 0 || index >= array.length) {
            return;
        }
        array[index].getAndIncrement();
    }

    public List<Long> fetchAndClearAll() {
        List<Long> listValues = new ArrayList<>();
        for (AtomicLong a : array) {
            listValues.add(a.longValue());
        }

        int bound = array.length;
        for (int x = 0; x < bound; x++) {
            array[x] = new AtomicLong();
        }

        return listValues;
    }
}
