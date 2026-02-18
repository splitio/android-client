package io.split.android.client.network;

import java.security.cert.X509Certificate;
import java.util.List;

public interface CertificatePinningFailureListener {

    void onCertificatePinningFailure(String host, List<X509Certificate> certificateChain);
}
