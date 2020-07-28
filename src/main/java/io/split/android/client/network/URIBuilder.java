package io.split.android.client.network;

import androidx.annotation.NonNull;

import com.google.common.base.Strings;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class URIBuilder {
    private URI mRootURI;
    private String mPath;
    private Map<String, String> mParams;
    private String mQueryString;

    public URIBuilder(@NonNull URI rootURI, String path) {
        mRootURI = checkNotNull(rootURI);
        String rootPath = mRootURI.getPath();
        if(path != null && rootPath != null) {
            mPath = String.format("%s/%s", rootPath, path);
            mPath = mPath.replace("///", "/");
            mPath = mPath.replace("//", "/");
        } else if (rootPath != null) {
            mPath = rootPath;
            mQueryString = rootURI.getQuery();
        } else {
            mPath = path;
        }
        mParams = new HashMap<>();
    }

    public URIBuilder(@NonNull URI rootURI) {
        this(rootURI, null);
    }

    public URIBuilder addParameter(@NonNull String param, @NonNull String value) {
        if (param != null && value != null) {
            mParams.put(param, value);
        }
        return this;
    }

    public URIBuilder defaultQueryString(@NonNull String queryString) {
        if (!Strings.isNullOrEmpty(queryString)) {
            mQueryString = queryString;
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

        if (!Strings.isNullOrEmpty(mQueryString)) {
            if (!Strings.isNullOrEmpty(params)) {
                params = mQueryString + "&" + params;
            } else {
                params = mQueryString;
            }
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
