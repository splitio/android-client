package io.split.android.client.streaming.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.utils.logger.Logger;

public class CompressionUtilProvider {
    Map<CompressionType, CompressionUtil> mCompressionUtils = new ConcurrentHashMap<>();

    public CompressionUtil get(CompressionType type) {
        CompressionUtil util = mCompressionUtils.get(type);
        return (util != null ? util : create(type));
    }

    // Using a method instead of a factory to avoid
    // a complex architecture.
    private CompressionUtil create(CompressionType type) {
        switch (type) {
            case NONE:
                return new CompressionUtil() {
                    @Override
                    public byte[] decompress(byte[] compressed) {
                        return compressed;
                    }
                };
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
