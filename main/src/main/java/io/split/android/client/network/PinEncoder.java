package io.split.android.client.network;

import androidx.annotation.NonNull;

interface PinEncoder {

    @NonNull
    byte[] encodeCertPin(String algorithm, byte[] encodedPublicKey);
}
