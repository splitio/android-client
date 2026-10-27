package io.split.android.client.perf;

import android.os.Build;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyStore;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;

/**
 * Device facts for a perf report: model, manufacturer, API level, fingerprint and the security level
 * of the SDK's KeyStore key. Shared by the perf instrumented tests.
 */
public final class PerfDevice {

    private static final String TAG = "SplitPerf";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    // SecureKeyStorageProvider's alias prefix is private; this is its value.
    private static final String ALIAS_PREFIX = "split_key_";

    private PerfDevice() {
    }

    /**
     * Describes this device for a perf report. {@code keystore} is "software" on an emulator. Otherwise
     * it is read from the existing key of {@code split_key_<sdkKey>}: "tee" or "strongbox" on API 31+
     * from KeyInfo.getSecurityLevel, "hardware" on API 23 to 30 from isInsideSecureHardware (StrongBox
     * and TEE cannot be told apart there), "software" when not in secure hardware, and "unknown" when
     * the key is absent or the probe fails. The probe never creates the key, and {@code sdkKey} is not
     * reported.
     */
    public static JSONObject describe(String sdkKey) throws JSONException {
        boolean emulator = looksLikeEmulator();
        String keystore = emulator ? "software" : probeKeystore(ALIAS_PREFIX + sdkKey);
        return new JSONObject()
                .put("model", nullToEmpty(Build.MODEL))
                .put("manufacturer", nullToEmpty(Build.MANUFACTURER))
                .put("apiLevel", Build.VERSION.SDK_INT)
                .put("fingerprint", nullToEmpty(Build.FINGERPRINT))
                .put("emulator", emulator)
                .put("keystore", keystore);
    }

    private static String probeKeystore(String alias) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return "unknown";
        }
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            if (!keyStore.containsAlias(alias)) {
                return "unknown";
            }
            SecretKey key = (SecretKey) keyStore.getKey(alias, null);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(key.getAlgorithm(), KEYSTORE_PROVIDER);
            KeyInfo info = (KeyInfo) factory.getKeySpec(key, KeyInfo.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return securityLevelName(info.getSecurityLevel());
            }
            return info.isInsideSecureHardware() ? "hardware" : "software";
        } catch (Exception e) {
            Log.w(TAG, "could not probe the KeyStore key: " + e);
            return "unknown";
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static String securityLevelName(int level) {
        switch (level) {
            case KeyProperties.SECURITY_LEVEL_SOFTWARE:
                return "software";
            case KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT:
                return "tee";
            case KeyProperties.SECURITY_LEVEL_STRONGBOX:
                return "strongbox";
            default:
                return "unknown";
        }
    }

    private static boolean looksLikeEmulator() {
        String fingerprint = nullToEmpty(Build.FINGERPRINT);
        String model = nullToEmpty(Build.MODEL);
        String product = nullToEmpty(Build.PRODUCT);
        String hardware = nullToEmpty(Build.HARDWARE);
        return fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || fingerprint.contains("emulator")
                || model.contains("Emulator")
                || model.contains("Android SDK built for")
                || model.contains("google_sdk")
                || product.contains("sdk")
                || product.contains("emulator")
                || hardware.equals("goldfish")
                || hardware.equals("ranchu");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
