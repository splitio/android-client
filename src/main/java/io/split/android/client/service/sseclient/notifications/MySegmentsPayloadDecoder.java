package io.split.android.client.service.sseclient.notifications;

import android.util.Base64;

import androidx.annotation.NonNull;

import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.MurmurHash3;
import io.split.android.client.utils.StringHelper;

public class MySegmentsPayloadDecoder {

    /**
     * @param matchingKey plain matching key.
     * @return Base64 encoding of {@param matchingKey} murmur3_32 hash.
     */
    @NonNull
    public String hashUserKeyForMySegmentsV1(String matchingKey) {
        try {
            long murmurHash = MurmurHash3.murmurhash3_x86_32(matchingKey, 0, matchingKey.length(), 0);
            byte[] murmurHashStringBytes = String.valueOf(murmurHash).getBytes(StringHelper.defaultCharset());

            return Base64.encodeToString(murmurHashStringBytes, Base64.NO_WRAP);
        } catch (Exception exception) {
            Logger.e("An error occurred when encoding matching key");

            return "";
        }
    }
}
