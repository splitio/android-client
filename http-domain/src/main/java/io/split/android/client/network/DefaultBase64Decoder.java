package io.split.android.client.network;

import android.util.Base64;

import io.split.android.client.utils.logger.Logger;

public class DefaultBase64Decoder implements Base64Decoder {

    @Override
    public byte[] decode(String base64) {
        try {
            return Base64.decode(base64, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            Logger.e("Received bytes didn't correspond to a valid Base64 encoded string." + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("An unknown error has occurred " + e.getLocalizedMessage());
        }
        return null;
    }
}
