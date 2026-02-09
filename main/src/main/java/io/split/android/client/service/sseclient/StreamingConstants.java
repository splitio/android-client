package io.split.android.client.service.sseclient;

/**
 * Constants used by the streaming module.
 */
public final class StreamingConstants {
    
    private StreamingConstants() {
        // Utility class
    }
    
    /**
     * Buffer size for segment data decompression.
     */
    public static final int SEGMENT_DATA_BUFFER_SIZE = 1024 * 10; // 10KB

    /**
     * Query param for flags spec in streaming auth.
     */
    public static final String FLAGS_SPEC_PARAM = "s";
}
