package io.split.android.client.service.impressions;

import io.split.android.client.impressions.Impression;
import io.split.android.client.utils.MurmurHash3;

public class ImpressionHasher {

    private static final String UNKNOWN = "UNKNOWN";
    private static final int SEED = 0;
    private static final int OFFSET = 0;

    private static String unknownIfNull(String s) {
        return (s == null) ? UNKNOWN : s;
    }

    private static Long zeroIfNull(Long l) {
        return (l == null) ? 0 : l;
    }

    public static Long process(Impression impression) {
        if (null == impression) {
            return null;
        }

        String data = unknownIfNull(impression.key()) +
                ":" +
                unknownIfNull(impression.split()) +
                ":" +
                unknownIfNull(impression.treatment()) +
                ":" +
                unknownIfNull(impression.appliedRule()) +
                ":" +
                zeroIfNull(impression.changeNumber());

        return MurmurHash3.murmurhash3_x86_32(data, OFFSET, data.length(), SEED);
    }
}
