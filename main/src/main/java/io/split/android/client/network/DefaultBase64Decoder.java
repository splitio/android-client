package io.split.android.client.network;

import io.split.android.client.utils.Base64Util;

class DefaultBase64Decoder implements Base64Decoder {

    @Override
    public byte[] decode(String base64) {
        return Base64Util.bytesDecode(base64);
    }
}
