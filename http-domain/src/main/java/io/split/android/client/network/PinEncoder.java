package io.split.android.client.network;

import androidx.annotation.NonNull;

public interface PinEncoder {

    @NonNull
    byte[] encodeCertPin(String algorithm, byte[] encodedPublicKey);
}
