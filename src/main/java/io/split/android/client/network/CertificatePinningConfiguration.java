package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.InputStream;
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
    static Builder builder(Base64Decoder base64Decoder, PinEncoder pinEncoder) {
        return new Builder(base64Decoder, pinEncoder);
    }

    public static class Builder {
        private final Map<String, Set<CertificatePin>> mPins = new LinkedHashMap<>();
        private CertificatePinningFailureListener mFailureListener;
        private final Base64Decoder mBase64Decoder;
        private final PinEncoder mPinEncoder;

        private Builder() {
            this(new DefaultBase64Decoder(), new PinEncoderImpl());
        }

        @VisibleForTesting
        Builder(Base64Decoder base64Decoder, PinEncoder pinEncoder) {
            mBase64Decoder = base64Decoder;
            mPinEncoder = pinEncoder;
        }

        /**
         * Adds a pin to the configuration.
         *
         * @param host The host to which the pin will be associated. It can be a full host or a wildcard host.
         *             Patterns like "*.example.com" or "**.example.com" are supported.
         *             A `**` pattern will match any number of subdomains.
         *             A `*` pattern will match one subdomain.
         * @param pin  The pin to be added. It must be in the form "[algorithm]/[hash]".
         *             The hash is a base64 encoded string.
         *             Supported algorithms are sha256 and sha1.
         *             For example: "sha256/AAAAAAA="
         * @return This builder.
         */
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

            if (!algorithm.equalsIgnoreCase(Algorithm.SHA256) && !algorithm.equalsIgnoreCase(Algorithm.SHA1)) {
                Logger.e("Invalid algorithm. Must be sha256 or sha1. Ignoring entry for host " + host);
                return this;
            }

            Set<CertificatePin> pins = getInitializedPins(host);
            pins.add(new CertificatePin(mBase64Decoder.decode(hash), algorithm));

            return this;
        }

        /**
         * Adds a pin to the configuration.
         *
         * @param host        The host to which the pin will be associated. It can be a full host or a wildcard host.
         *                    Patterns like "*.example.com" or "**.example.com" are supported.
         *                    A `**` pattern will match any number of subdomains.
         *                    A `*` pattern will match one subdomain.
         * @param inputStream The {@link InputStream} containing certificate data to be used to calculate the pin hashes.
         *                    This is useful for specifying a certificate file.
         *                    If the data contains the certificate chain, a pin for each certificate in the chain will be added.
         *                    The stream will be closed after reading the data.
         * @return This builder.
         */
        public Builder addPin(String host, InputStream inputStream) {
            if (host == null || host.trim().isEmpty()) {
                Logger.e("Host cannot be null or empty. Ignoring entry");
                return this;
            }

            if (inputStream == null) {
                Logger.e("InputStream cannot be null. Ignoring entry for host " + host);
            }

            Set<CertificatePin> pins = getInitializedPins(host);

            Set<CertificatePin> newPins = CertificateCheckerHelper.getPinsFromInputStream(inputStream, mPinEncoder);
            if (newPins.isEmpty()) {
                Logger.e("No pins found in input stream. Ignoring entry for host " + host);
                return this;
            }

            pins.addAll(newPins);

            return this;
        }

        /**
         * Sets the listener to be called when a certificate pinning failure occurs.
         *
         * @param failureListener The listener to be called.
         * @return This builder.
         */
        public Builder failureListener(@NonNull CertificatePinningFailureListener failureListener) {
            if (failureListener == null) { // just in case
                Logger.w("Failure listener cannot be null");
                return this;
            }
            mFailureListener = failureListener;
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

                if (!pin.getAlgorithm().equalsIgnoreCase(Algorithm.SHA256) && !pin.getAlgorithm().equalsIgnoreCase(Algorithm.SHA1)) {
                    Logger.e("Invalid algorithm. Must be sha256 or sha1. Ignoring entry for host " + host);
                    continue;
                }

                validPins.add(pin);
            }

            if (!validPins.isEmpty()) {
                mPins.put(host, validPins);
            }
        }

        /**
         * Builds the configuration.
         *
         * @return The configuration.
         */
        public CertificatePinningConfiguration build() {
            return new CertificatePinningConfiguration(this);
        }

        @NonNull
        private Set<CertificatePin> getInitializedPins(String host) {
            Set<CertificatePin> pins = mPins.get(host);
            if (pins == null) {
                pins = new HashSet<>();
                mPins.put(host, pins);
            }
            return pins;
        }

        private static class DefaultBase64Decoder implements Base64Decoder {
            @Override
            public byte[] decode(String base64) {
                return Base64Util.bytesDecode(base64);
            }
        }
    }
}
