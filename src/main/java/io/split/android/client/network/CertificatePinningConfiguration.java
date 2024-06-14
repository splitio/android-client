package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.logger.Logger;

public class CertificatePinningConfiguration {

    private final Map<String, Set<CertificatePin>> mPins;
    private final CertificatePinningFailureListener mFailureListener;

    private CertificatePinningConfiguration() {
        this(new Builder());
    }

    private CertificatePinningConfiguration(Map<String, Set<CertificatePin>> pins, CertificatePinningFailureListener failureListener) {
        mPins = pins;
        mFailureListener = failureListener;
    }

    private CertificatePinningConfiguration(Builder builder) {
        this(builder.mPins, builder.mFailureListener);
    }

    @NonNull
    public Map<String, Set<CertificatePin>> getPins() {
        return mPins;
    }

    public CertificatePinningFailureListener getFailureListener() {
        return mFailureListener;
    }

    public static Builder builder() {
        return new Builder();
    }

    @VisibleForTesting
    static Builder builder(Base64Decoder base64Decoder) {
        return new Builder(base64Decoder);
    }

    public static class Builder {
        private final Map<String, Set<CertificatePin>> mPins = new LinkedHashMap<>();
        private CertificatePinningFailureListener mFailureListener;
        private final Base64Decoder mBase64Decoder;

        private Builder() {
            this(new DefaultBase64Decoder());
        }

        @VisibleForTesting
        Builder(Base64Decoder base64Decoder) {
            mBase64Decoder = base64Decoder;
        }

        public Builder addPin(String host, String pin) {
            if (host == null || host.trim().isEmpty()) {
                Logger.e("Host cannot be null or empty. Ignoring entry");
                return this;
            }

            if (pin == null || pin.trim().isEmpty()) {
                Logger.e("Pin cannot be null or empty. Ignoring entry for host " + host);
                return this;
            }

            String[] parts = pin.split("/", 2);
            if (parts.length != 2) {
                Logger.e("Pin must be in the form \"[algorithm]/[hash]\". Ignoring entry for host " + host);
                return this;
            }

            String hash = parts[1];
            String algorithm = parts[0];

            if (!algorithm.equalsIgnoreCase("sha256") && !algorithm.equalsIgnoreCase("sha1")) {
                Logger.e("Invalid algorithm. Must be sha256 or sha1. Ignoring entry for host " + host);
                return this;
            }

            Set<CertificatePin> pins = mPins.get(host);
            if (pins == null) {
                pins = new HashSet<>();
                mPins.put(host, pins);
            }
            pins.add(new CertificatePin(mBase64Decoder.decode(hash), algorithm));

            return this;
        }

        // Meant to be used only when setting up bg sync jobs
        void addPins(String host, Set<CertificatePin> pins) {
            if (host == null || host.trim().isEmpty()) {
                Logger.e("Host cannot be null or empty. Ignoring entry");
                return;
            }

            if (pins == null || pins.isEmpty()) {
                Logger.e("Pins cannot be null or empty. Ignoring entry for host " + host);
                return;
            }

            Set<CertificatePin> validPins = new HashSet<>();
            for (CertificatePin pin : pins) {
                if (pin == null) {
                    Logger.e("Pin cannot be null. Ignoring entry for host " + host);
                    continue;
                }

                if (!pin.getAlgorithm().equalsIgnoreCase("sha256") && !pin.getAlgorithm().equalsIgnoreCase("sha1")) {
                    Logger.e("Invalid algorithm. Must be sha256 or sha1. Ignoring entry for host " + host);
                    continue;
                }

                validPins.add(pin);
            }

            if (!validPins.isEmpty()) {
                mPins.put(host, validPins);
            }
        }

        public Builder failureListener(@NonNull CertificatePinningFailureListener failureListener) {
            if (failureListener == null) { // just in case
                Logger.w("Failure listener cannot be null");
                return this;
            }
            mFailureListener = failureListener;
            return this;
        }

        public CertificatePinningConfiguration build() {
            return new CertificatePinningConfiguration(this);
        }

        private static class DefaultBase64Decoder implements Base64Decoder {
            @Override
            public byte[] decode(String base64) {
                return Base64Util.bytesDecode(base64);
            }
        }
    }
}
