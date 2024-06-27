package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.split.android.client.utils.logger.Logger;

class CertificateCheckerHelper {

    @Nullable
    static Set<CertificatePin> getPinsForHost(String pattern, Map<String, Set<CertificatePin>> configuredPins) {
        Set<CertificatePin> hostPins = configuredPins.get(pattern);
        Set<CertificatePin> wildcardPins = new LinkedHashSet<>();

        for (String configuredHost : configuredPins.keySet()) {
            if (configuredHost.startsWith("**.")) {
                String configuredSubdomain = configuredHost.substring(3);
                if (pattern.regionMatches(pattern.length() - configuredSubdomain.length(), configuredSubdomain, 0, configuredSubdomain.length())) {
                    wildcardPins.addAll(configuredPins.get(configuredHost));
                }
            } else if (configuredHost.startsWith("*.")) {
                String configuredSubdomain = configuredHost.substring(2);
                int index = pattern.lastIndexOf(configuredSubdomain);
                if (index != -1 && pattern.charAt(index - 1) == '.' &&
                        pattern.regionMatches(index, configuredSubdomain, 0, configuredSubdomain.length())) {
                    String[] hostParts = pattern.substring(0, index - 1).split("\\.");
                    if (hostParts.length == 1) {
                        wildcardPins.addAll(configuredPins.get(configuredHost));
                    }
                }
            }
        }

        if (hostPins == null && wildcardPins.isEmpty()) {
            return null;
        }

        if (hostPins != null) {
            wildcardPins.addAll(hostPins);
        }

        return wildcardPins;
    }

    @NonNull
    static Set<CertificatePin> getPinsFromInputStream(InputStream inputStream, PinEncoder pinEncoder) {
        try (InputStream stream = inputStream) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");

            Collection<? extends Certificate> certificates = factory.generateCertificates(stream);
            Set<CertificatePin> pins = new LinkedHashSet<>();
            for (Certificate certificate : certificates) {
                if (certificate instanceof X509Certificate) {
                    pins.add(new CertificatePin(pinEncoder.encodeCertPin(
                            Algorithm.SHA256,
                            certificate.getPublicKey().getEncoded()), Algorithm.SHA256));
                }
            }

            return pins;
        } catch (Exception e) {
            Logger.e("Error parsing certificate pins from input stream: " + e.getLocalizedMessage());
            return new HashSet<>();
        }
    }
}
