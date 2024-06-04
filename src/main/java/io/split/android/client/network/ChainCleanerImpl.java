package io.split.android.client.network;

import android.net.http.X509TrustManagerExtensions;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.X509TrustManager;

import io.split.android.client.utils.logger.Logger;

class ChainCleanerImpl implements ChainCleaner {

    private final X509TrustManagerExtensions mTrustManagerExtensions;

    ChainCleanerImpl() {
        this(new X509TrustManagerExtensions(TrustManagerProvider.getDefaultX509TrustManager()));
    }

    @VisibleForTesting
    ChainCleanerImpl(X509TrustManager trustManager) {
        this(new X509TrustManagerExtensions(trustManager));
    }

    @VisibleForTesting
    ChainCleanerImpl(X509TrustManagerExtensions trustManagerExtensions) {
        mTrustManagerExtensions = trustManagerExtensions;
    }

    @Override
    @NonNull
    public List<X509Certificate> clean(String hostname, Certificate[] chain) {
        if (chain == null) {
            return Collections.emptyList();
        }

        try {
            List<X509Certificate> x509Certificates = new ArrayList<>();
            for (Certificate certificate : chain) {
                try {
                    x509Certificates.add((X509Certificate) certificate);
                } catch (ClassCastException e) {
                    Logger.v("Ignored non-X.509 certificate in chain cleaning");
                }
            }

            return mTrustManagerExtensions.checkServerTrusted(x509Certificates.toArray(new X509Certificate[0]), "RSA", hostname);
        } catch (CertificateException e) {
            return Collections.emptyList();
        }
    }
}
