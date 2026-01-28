package io.split.android.client.network;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class CertificatePinningConfigurationProvider {

    public static CertificatePinningConfiguration getCertificatePinningConfiguration(String pinsJson) {
        try {
            Type type = new TypeToken<Map<String, Set<CertificatePin>>>() {
            }.getType();
            Map<String, Set<CertificatePin>> certificatePins = Json.fromJson(pinsJson, type);

            if (certificatePins != null && !certificatePins.isEmpty()) {
                CertificatePinningConfiguration.Builder builder = CertificatePinningConfiguration.builder();
                for (Map.Entry<String, Set<CertificatePin>> entry : certificatePins.entrySet()) {
                    builder.addPins(entry.getKey(), entry.getValue());
                }

                return builder
                        .build();
            }
        } catch (Exception e) {
            Logger.e("Error parsing certificate pinning configuration for background sync worker", e.getLocalizedMessage());
        }

        return null;
    }
}
