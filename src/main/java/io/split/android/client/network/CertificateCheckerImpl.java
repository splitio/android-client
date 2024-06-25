package io.split.android.client.network;

import static io.split.android.client.network.CertificateCheckerHelper.getPinsForHost;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.X509TrustManager;

import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.logger.Logger;

class CertificateCheckerImpl implements CertificateChecker {

    @NonNull
    private final Map<String, Set<CertificatePin>> mConfiguredPins;
    @Nullable
    private final CertificatePinningFailureListener mFailureListener;
    @NonNull
    private final ChainCleaner mChainCleaner;
    @NonNull
    private final Base64Encoder mBase64Encoder;
    @NonNull
    private final PinEncoder mPinEncoder;

    CertificateCheckerImpl(CertificatePinningConfiguration certificatePinningConfiguration, @Nullable X509TrustManager trustManager) {
        this(certificatePinningConfiguration.getPins(), certificatePinningConfiguration.getFailureListener(), new ChainCleanerImpl(trustManager), new DefaultBase64Encoder(), new PinEncoderImpl());
    }

    @VisibleForTesting
    CertificateCheckerImpl(@Nullable Map<String, Set<CertificatePin>> configuredPins,
                           @Nullable CertificatePinningFailureListener failureListener,
                           @NonNull ChainCleaner chainCleaner,
                           @NonNull Base64Encoder base64Encoder,
                           @NonNull PinEncoder pinEncoder) {
        mConfiguredPins = configuredPins != null ? configuredPins : new HashMap<>();
        mFailureListener = failureListener;
        mChainCleaner = chainCleaner;
        mBase64Encoder = base64Encoder;
        mPinEncoder = pinEncoder;
    }

    @Override
    public void checkPins(HttpsURLConnection httpsConnection) throws SSLPeerUnverifiedException {
        String host = httpsConnection.getURL().getHost();
        Set<CertificatePin> pinsForHost = getPinsForHost(host, mConfiguredPins);
        if (pinsForHost == null || pinsForHost.isEmpty()) {
            Logger.d("No certificate pins configured for " + host + ". Skipping pinning verification.");
            return;
        }

        List<X509Certificate> cleanCertificates;
        try {
            cleanCertificates = mChainCleaner.clean(host, httpsConnection.getServerCertificates());
        } catch (Exception e) {
            throw new SSLPeerUnverifiedException("Error cleaning certificate chain for host: " + host);
        }

        for (X509Certificate certificate : cleanCertificates) {
            for (CertificatePin pinnedCertificate : pinsForHost) {
                byte[] pin = mPinEncoder.encodeCertPin(
                        pinnedCertificate.getAlgorithm(),
                        certificate.getPublicKey().getEncoded());
                if (Arrays.equals(pin, pinnedCertificate.getPin())) {
                    Logger.v("Certificate pinning verification successful for " + host);
                    return;
                }
            }
        }

        if (mFailureListener != null) {
            mFailureListener.onCertificatePinningFailure(host, cleanCertificates);
        }

        throw new SSLPeerUnverifiedException("Certificate pinning verification failed for host: " + host + ". Chain:\n" + certificateChainInfo(cleanCertificates));
    }

    private String certificateChainInfo(List<X509Certificate> cleanCertificates) {
        StringBuilder builder = new StringBuilder();
        for (X509Certificate certificate : cleanCertificates) {
            builder.append(certificate.getSubjectDN().getName()).append(" - ")
                    .append("sha256/")
                    .append(mBase64Encoder.encode(mPinEncoder.encodeCertPin("sha256",
                            certificate.getPublicKey().getEncoded())));
        }

        return builder.toString();
    }

    private static class DefaultBase64Encoder implements Base64Encoder {

        @Override
        public String encode(String value) {
            return Base64Util.encode(value);
        }

        @Override
        public String encode(byte[] bytes) {
            return Base64Util.encode(bytes);
        }
    }
}
