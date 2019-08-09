package io.split.android.client.utils;

public interface MemoryUtils {
    boolean isMemoryAvailableToAllocate(long bytes, int times);
    boolean isMemoryAvailableForJson(String json);
}
