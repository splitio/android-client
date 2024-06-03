package io.split.android.client.network;

import java.util.Arrays;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CertificatePin that = (CertificatePin) o;
        return Arrays.equals(mPin, that.mPin) && Objects.equals(mAlgorithm, that.mAlgorithm);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mAlgorithm);
        result = 31 * result + Arrays.hashCode(mPin);
        return result;
    }
}
