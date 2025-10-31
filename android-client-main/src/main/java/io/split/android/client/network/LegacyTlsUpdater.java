package io.split.android.client.network;

import android.content.Context;
import android.os.Build;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import io.split.android.client.utils.logger.Logger;

public class LegacyTlsUpdater {
    private static final String TLS_VERSION = "TLSv1.2";

    public static boolean couldBeOld() {
        return  Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    public static void update(Context context) {
        if(context == null) {
            return;
        }
        try {
            SSLContext.getInstance(TLS_VERSION);
        } catch (NoSuchAlgorithmException e) {
            try {
                Logger.i("TLS v1.2 is not available, installing...");
                ProviderInstaller.installIfNeeded(context.getApplicationContext());
            } catch (GooglePlayServicesRepairableException e1) {
                Logger.e("Couldn't install TLS v1.2 Google Play Services version is very old.");
            } catch (GooglePlayServicesNotAvailableException e1) {
                Logger.e("Couldn't update TLS version. Google Play Services is not available.");
            }
        }
    }
}


