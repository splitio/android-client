package io.split.android.client.network;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class CertificatePinningConfigurationProvider {

    public static CertificatePinningConfiguration getCertificatePinningConfiguration(String pinsJson) {
        try {
            Type type = new TypeToken<Map<String, Set<CertificatePinDto>>>() {
            }.getType();
            Map<String, Set<CertificatePinDto>> certificatePins = Json.fromJson(pinsJson, type);

            if (certificatePins != null && !certificatePins.isEmpty()) {
                CertificatePinningConfiguration.Builder builder = CertificatePinningConfiguration.builder();
                for (Map.Entry<String, Set<CertificatePinDto>> entry : certificatePins.entrySet()) {
                    Set<CertificatePin> pins = new HashSet<>();
                    for (CertificatePinDto dto : entry.getValue()) {
                        pins.add(new CertificatePin(dto.pin, dto.algorithm));
                    }
                    builder.addPins(entry.getKey(), pins);
                }

                return builder
                        .build();
            }
        } catch (Exception e) {
            Logger.e("Error parsing certificate pinning configuration for background sync worker", e.getLocalizedMessage());
        }

        return null;
    }

    private static class CertificatePinDto {
        @SerializedName("pin")
        byte[] pin;
        @SerializedName("algo")
        String algorithm;
    }
}
