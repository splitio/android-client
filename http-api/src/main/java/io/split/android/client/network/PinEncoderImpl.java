package io.split.android.client.network;

import androidx.annotation.NonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.split.android.client.utils.logger.Logger;

class PinEncoderImpl implements PinEncoder {

    @Override
    @NonNull
    public byte[] encodeCertPin(String algorithm, byte[] encodedPublicKey) {
        switch (algorithm) {
            case "sha256":
                return sha256Hash(encodedPublicKey);
            case "sha1":
                return sha1Hash(encodedPublicKey);
            default:
                return new byte[]{};
        }
    }

    private static byte[] sha256Hash(byte[] encoded) {
        MessageDigest digest = getDigest("SHA-256");

        if (digest != null) {
            return digest.digest(encoded);
        } else {
            return new byte[0];
        }
    }

    private static byte[] sha1Hash(byte[] encoded) {
        MessageDigest digest = getDigest("SHA-1");

        if (digest != null) {
            return digest.digest(encoded);
        } else {
            return new byte[0];
        }
    }

    private static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            Logger.e("Error getting " + algorithm + " MessageDigest: " + e.getMessage());
            return null;
        }
    }
}
