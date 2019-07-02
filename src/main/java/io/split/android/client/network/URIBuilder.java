package io.split.android.client.network;

import android.net.Uri;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class URIBuilder {
    private URI mRootURI;
    private String mPath;
    private Map<String, String> mParams;

    public URIBuilder(URI rootURI, String path) {
        mRootURI = rootURI;
        mPath = path;
        mParams = new HashMap<>();
    }

    public URIBuilder(URI rootURI) {
        this(rootURI, null);
    }

    public URIBuilder addParameter(String param, String value) {
        if (param != null && value != null) {
            mParams.put(param, value);
        }
        return this;
    }

    public URI build() throws URISyntaxException {

        String params = null;
        if (mParams.size() > 0) {
            StringBuilder query = new StringBuilder();
            for (Map.Entry<String, String> param : mParams.entrySet()) {
                query.append(param.getKey() + "=" + param.getValue() + "&");
            }
            params = query.substring(0, query.length() - 1).toString();
        }

        return new URI(mRootURI.getScheme(),
                null,
                mRootURI.getHost(),
                mRootURI.getPort(),
                mPath,
                params,
                null);
    }
}
