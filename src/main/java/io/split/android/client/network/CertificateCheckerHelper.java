package io.split.android.client.network;

import androidx.annotation.Nullable;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
}
