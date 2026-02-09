package io.split.android.client.network;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class URIBuilder {
    private final URI mRootURI;
    private final Set<Map.Entry<String, String>> mParams;
    private String mPath;
    private String mQueryString;

    public URIBuilder(@NonNull URI rootURI, String path) {
        mRootURI = requireNonNull(rootURI);
        String rootPath = mRootURI.getRawPath();
        if (path != null && rootPath != null) {
            mPath = String.format("%s/%s", rootPath, path);
            mPath = mPath.replace("///", "/");
            mPath = mPath.replace("//", "/");
        } else if (rootPath != null) {
            mPath = rootPath;
            mQueryString = rootURI.getQuery();
        } else {
            mPath = path;
        }
        mParams = new LinkedHashSet<>();
    }

    public URIBuilder(@NonNull URI rootURI) {
        this(rootURI, null);
    }

    public URIBuilder addParameter(@NonNull String param, @NonNull String value) {
        if (param != null && value != null) {
            mParams.add(new AbstractMap.SimpleEntry<>(param, value));
        }
        return this;
    }

    public URIBuilder defaultQueryString(@NonNull String queryString) {
        if (queryString != null && !queryString.isEmpty()) {
            mQueryString = queryString;
        }
        return this;
    }

    public URI build() throws URISyntaxException {

        String params = null;
        if (mParams.size() > 0) {
            StringBuilder query = new StringBuilder();
            for (Map.Entry<String, String> param : mParams) {
                query.append(param.getKey()).append("=").append(param.getValue()).append("&");
            }
            params = query.substring(0, query.length() - 1);
        }

        if (mQueryString != null && !mQueryString.isEmpty()) {
            if (params != null && !params.isEmpty()) {
                if (!"&".equals(mQueryString.substring(0, 1))) {
                    params = params + "&";
                }
                params = params + mQueryString;
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
