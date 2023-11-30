package io.split.android.client.network;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

class SplitUrlConnectionAuthenticator {

    private final SplitAuthenticator mProxyAuthenticator;

    SplitUrlConnectionAuthenticator(SplitAuthenticator splitAuthenticator) {
        mProxyAuthenticator = splitAuthenticator;
    }

    HttpURLConnection authenticate(HttpURLConnection connection) {
        SplitAuthenticatedRequest authenticatedRequest = mProxyAuthenticator.authenticate(new SplitAuthenticatedRequest(connection));
        if (authenticatedRequest != null) {
            Map<String, List<String>> headers = authenticatedRequest.getHeaders();

            if (headers != null) {
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    if (entry == null) {
                        continue;
                    }

                    for (String value : entry.getValue()) {
                        connection.addRequestProperty(entry.getKey(), value);
                    }
                }
            }
        }

        return connection;
    }
}
