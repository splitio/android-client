package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

import io.split.android.client.utils.logger.Logger;

class CertificateCheckerImpl implements CertificateChecker {

    @NonNull
    private final Map<String, List<CertificatePin>> mConfiguredPins;
    @Nullable
    private final CertificatePinningFailureListener mFailureListener;
    @NonNull
    private final ChainCleaner mChainCleaner;
    @NonNull
    private final Base64Encoder mBase64Encoder;
    @NonNull
    private final PinEncoder mPinEncoder;

//    public CertificateCheckerImpl(@NonNull CertificatePinningConfiguration configuration) {
//        this(configuration.getPins(), configuration.getFailureStrategy(), new ChainCleaner() {
//
//            @Override
//            public List<X509Certificate> clean(String host, Certificate[] chain) {
//                return Collections.emptyList();
//                // TODO
//            }
//        }, new Base64Encoder() { // TODO
//
//            @Override
//            public String encode(String value) {
//                return Base64Util.encode(value);
//            }
//
//            @Override
//            public String encode(byte[] bytes) {
//                return Base64Util.encode(bytes);
//            }
//        }, new PinEncoderImpl());
//    }

    @VisibleForTesting
    CertificateCheckerImpl(@Nullable Map<String, List<CertificatePin>> configuredPins,
                                   @Nullable CertificatePinningFailureListener failureStrategy,
                                   @NonNull ChainCleaner chainCleaner,
                                   @NonNull Base64Encoder base64Encoder,
                                   @NonNull PinEncoder pinEncoder) {
        mConfiguredPins = configuredPins != null ? configuredPins : new HashMap<>();
        mFailureListener = failureStrategy;
        mChainCleaner = chainCleaner;
        mBase64Encoder = base64Encoder;
        mPinEncoder = pinEncoder;
    }

    @Override
    public void checkPins(HttpsURLConnection httpsConnection) throws SSLPeerUnverifiedException {
        try {
            String host = httpsConnection.getURL().getHost();
            List<CertificatePin> pinsForHost = getPinsForHost(host);
            if (pinsForHost == null || pinsForHost.isEmpty()) {
                Logger.d("No certificate pins configured for " + host + ". Skipping pinning verification.");
                return;
            }

            List<X509Certificate> cleanCertificates = mChainCleaner.clean(host, httpsConnection.getServerCertificates());

            for (X509Certificate certificate : cleanCertificates) {
                for (CertificatePin pinnedCertificate : pinsForHost) {
                    byte[] pin = mPinEncoder.encodeCertPin(
                            pinnedCertificate.getAlgorithm(),
                            certificate.getPublicKey().getEncoded());
                    if (Arrays.equals(pin, pinnedCertificate.getPin())) {
                        Logger.d("Certificate pinning verification successful for " + host);
                        return;
                    }
                }
            }

            if (mFailureListener != null) {
                mFailureListener.onCertificatePinningFailure(host, cleanCertificates);
            }

            throw new SSLPeerUnverifiedException("Certificate pinning verification failed for host: " + host + ". Chain:\n" + certificateChainInfo(host, cleanCertificates));
        } catch (Exception e) {
            if (e instanceof SSLPeerUnverifiedException) {
                throw e;
            }
            throw new SSLPeerUnverifiedException("Error checking certificate pins: " + e.getMessage());
        }
    }

    private List<CertificatePin> getPinsForHost(String host) {
        return mConfiguredPins.get(host);
    }

    private String certificateChainInfo(String host, List<X509Certificate> cleanCertificates) {
        StringBuilder builder = new StringBuilder();
        for (X509Certificate certificate : cleanCertificates) {
            builder.append(certificate.getSubjectDN().getName()).append(" - ")
                    .append("sha256/")
                    .append(mBase64Encoder.encode(mPinEncoder.encodeCertPin("sha256",
                            certificate.getPublicKey().getEncoded())))
                    .append("\n");
        }

        return builder.toString();
    }
}
