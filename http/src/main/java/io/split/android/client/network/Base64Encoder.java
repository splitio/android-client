package io.split.android.client.network;

interface Base64Encoder {

    String encode(String value);

    String encode(byte[] bytes);
}
