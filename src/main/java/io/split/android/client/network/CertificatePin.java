package io.split.android.client.network;

public class CertificatePin {

    private final byte[] mPin;
    private final String mAlgorithm;

    CertificatePin(byte[] pin, String algorithm) {
        mPin = pin;
        mAlgorithm = algorithm;
    }

    public byte[] getPin() {
        return mPin;
    }

    public String getAlgorithm() {
        return mAlgorithm;
    }
}
