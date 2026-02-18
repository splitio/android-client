package io.split.android.client.network;

import android.util.Base64;

class DefaultBase64Encoder implements Base64Encoder {

    @Override
    public String encode(String value) {
        if (value == null) {
            return null;
        }
        return Base64.encodeToString(value.getBytes(), Base64.NO_WRAP);
    }

    @Override
    public String encode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
}
