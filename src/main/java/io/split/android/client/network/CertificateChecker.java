package io.split.android.client.network;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

public interface CertificateChecker {

    void checkPins(HttpsURLConnection httpsConnection) throws SSLPeerUnverifiedException;
}
