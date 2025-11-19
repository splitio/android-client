package io.split.android.client.utils;

public class MemoryUtilsImpl implements MemoryUtils {
    private static final int MEMORY_ALLOCATION_TIMES_FOR_JSON = 2;

    public boolean isMemoryAvailableToAllocate(long bytes, int times) {
        return Runtime.getRuntime().freeMemory() > bytes * times;
    }

    public boolean isMemoryAvailableForJson(String json) {
        if (Utils.isNullOrEmpty(json)) {
            return true;
        }
        return isMemoryAvailableToAllocate(json.getBytes().length, MEMORY_ALLOCATION_TIMES_FOR_JSON);
    }
}
