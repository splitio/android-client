package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class SplitBasicAuthenticator extends SplitAuthenticator {

    private static final String PROXY_AUTHORIZATION_HEADER = "Proxy-Authorization";
    private final String mUsername;
    private final String mPassword;
    @NonNull
    private final Base64Encoder mBase64Encoder;

    SplitBasicAuthenticator(String username, String password, @NonNull Base64Encoder base64Encoder) {
        mUsername = username;
        mPassword = password;
        mBase64Encoder = base64Encoder;
    }

    @Nullable
    @Override
    public SplitAuthenticatedRequest authenticate(@NonNull SplitAuthenticatedRequest request) {
        String credential = basic(mUsername, mPassword);
        request.setHeader(PROXY_AUTHORIZATION_HEADER, credential);

        return request;
    }

    private String basic(String username, String password) {
        String usernameAndPassword = username + ":" + password;
        String encoded = mBase64Encoder.encode(usernameAndPassword);
        return "Basic " + encoded;
    }
}
