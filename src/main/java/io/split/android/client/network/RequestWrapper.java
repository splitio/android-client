package io.split.android.client.network;

import java.net.URLConnection;

public class RequestWrapper implements AuthenticatedRequest<URLConnection> {

    private final URLConnection mRequest;

    public RequestWrapper(URLConnection request) {
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
    public URLConnection getRequest() {
        return mRequest;
    }
}
