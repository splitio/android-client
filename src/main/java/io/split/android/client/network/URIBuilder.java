package io.split.android.client.network;

import android.support.annotation.NonNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class URIBuilder {
    private URI mRootURI;
    private String mPath;
    private Map<String, String> mParams;

    public URIBuilder(@NonNull URI rootURI, String path) {
        checkNotNull(rootURI);
        mRootURI = rootURI;
        String rootPath = mRootURI.getPath();
        if(path != null && rootPath != null) {
            mPath = String.format("%s/%s", rootPath, path);
            mPath = mPath.replace("///", "/");
            mPath = mPath.replace("//", "/");
        } else if (rootPath != null) {
            mPath = rootPath;
        } else {
            mPath = path;
        }
        mParams = new HashMap<>();
    }

    public URIBuilder(@NonNull URI rootURI) {
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
                query.append(param.getKey()).append("=").append(param.getValue()).append("&");
            }
            params = query.substring(0, query.length() - 1);
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
