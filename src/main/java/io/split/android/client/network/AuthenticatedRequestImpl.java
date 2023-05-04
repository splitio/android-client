package io.split.android.client.network;

import java.net.HttpURLConnection;

class AuthenticatedRequestImpl implements AuthenticatedRequest<HttpURLConnection> {

    private final HttpURLConnection mRequest;

    public AuthenticatedRequestImpl(HttpURLConnection request) {
        mRequest = request;
    }

    @Override
    public void setHeader(String name, String value) {
        mRequest.setRequestProperty(name, value);
    }

    @Override
    public String getHeader(String name) {
        return mRequest.getRequestProperty(name);
    }

    @Override
    public HttpURLConnection getRequest() {
        return mRequest;
    }
}
