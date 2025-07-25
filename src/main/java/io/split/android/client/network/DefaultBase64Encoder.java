package io.split.android.client.network;

import io.split.android.client.utils.Base64Util;

class DefaultBase64Encoder implements Base64Encoder {

    @Override
    public String encode(String value) {
        return Base64Util.encode(value);
    }

    @Override
    public String encode(byte[] bytes) {
        return Base64Util.encode(bytes);
    }
}
