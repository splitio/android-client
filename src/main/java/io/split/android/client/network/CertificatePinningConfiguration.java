package io.split.android.client.network;

import java.util.List;
import java.util.Map;

// TODO: Stub class
public abstract class CertificatePinningConfiguration {

    public abstract Map<String, List<CertificatePin>> getPins();

    public abstract CertificatePinningFailureListener getFailureStrategy();
}
