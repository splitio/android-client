package io.split.android.client.network;

import java.net.HttpURLConnection;
import java.util.Map;

class SplitUrlConnectionAuthenticator {

    private final SplitAuthenticator mProxyAuthenticator;

    SplitUrlConnectionAuthenticator(SplitAuthenticator splitAuthenticator) {
        mProxyAuthenticator = splitAuthenticator;
    }

    HttpURLConnection authenticate(HttpURLConnection connection) {
        SplitAuthenticatedRequest authenticatedRequest = mProxyAuthenticator.authenticate(new SplitAuthenticatedRequest(connection));
        if (authenticatedRequest != null) {
            Map<String, String> headers = authenticatedRequest.getHeaders();

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                        continue;
                    }

                    connection.addRequestProperty(entry.getKey(), entry.getValue());
                }

                return connection;
            }
        }

        return connection;
    }
}
