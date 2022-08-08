package io.split.android.client.network;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.google.common.base.Strings;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

public class URIBuilder {
    private final URI mRootURI;
    private final Set<Pair<String, String>> mParams;
    private String mPath;
    private String mQueryString;

    public URIBuilder(@NonNull URI rootURI, String path) {
        mRootURI = checkNotNull(rootURI);
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
        mParams = new HashSet<>();
    }

    public URIBuilder(@NonNull URI rootURI) {
        this(rootURI, null);
    }

    public URIBuilder addParameter(@NonNull String param, @NonNull String value) {
        if (param != null && value != null) {
            mParams.add(new Pair<>(param, value));
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
            for (Pair<String, String> param : mParams) {
                query.append(param.first).append("=").append(param.second).append("&");
            }
            params = query.substring(0, query.length() - 1);
        }

        if (!Strings.isNullOrEmpty(mQueryString)) {
            if (!Strings.isNullOrEmpty(params)) {
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
