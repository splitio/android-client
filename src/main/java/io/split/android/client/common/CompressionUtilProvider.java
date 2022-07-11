package io.split.android.client.common;

import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.utils.CompressionUtil;
import io.split.android.client.utils.Gzip;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.Zlib;

public class CompressionUtilProvider {
    Map<CompressionType, CompressionUtil> mCompressionUtils = new ConcurrentHashMap<>();

    @Nullable
    public CompressionUtil get(CompressionType type) {
        CompressionUtil util = mCompressionUtils.get(type);
        return (util != null ? util : create(type));
    }

    // Using a method instead of a factory to avoid
    // a complex architecture.
    @Nullable
    private CompressionUtil create(CompressionType type) {
        switch (type) {
            case GZIP:
                return new Gzip();
            case ZLIB:
                return new Zlib();
            default:
                Logger.d("Unavailable compression algorithm: " + type);
        }
        return null;
    }
}
