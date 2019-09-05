package io.split.android.fake;

import io.split.android.client.utils.MemoryUtils;

public class MemoryUtilsNoMemoryStub implements MemoryUtils {
    @Override
    public boolean isMemoryAvailableToAllocate(long bytes, int times) {
        return false;
    }

    @Override
    public boolean isMemoryAvailableForJson(String json) {
        return false;
    }
}
