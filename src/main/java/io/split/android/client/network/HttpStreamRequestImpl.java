package io.split.android.client.network;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import io.split.android.client.utils.logger.Logger;

public class HttpStreamRequestImpl implements HttpStreamRequest {

    private final URI mUri;
    private final Map<String, String> mHeaders;
    private final SSLSocketFactory mSslSocketFactory;
    private HttpURLConnection mConnection;

    HttpStreamRequestImpl(@NonNull URI uri,
                          @NonNull Map<String, String> headers,
                          SSLSocketFactory sslSocketFactory) {
        mUri = checkNotNull(uri);
        mHeaders = new HashMap<>(checkNotNull(headers));
        mSslSocketFactory = sslSocketFactory;
    }

    @Override
    public HttpStreamResponse execute() throws HttpException {
        return getRequest();
    }

    @Override
    public void addHeader(String name, String value) {
        mHeaders.put(name, value);
    }

    @Override
    public void close() {
        Logger.d("Closing streaming connection");
        mConnection.disconnect();
    }

    private HttpStreamResponse getRequest() throws HttpException {
        URL url;
        HttpStreamResponse response;
        try {
            url = mUri.toURL();
            mConnection = (HttpURLConnection) url.openConnection();
            addHeaders(mConnection);

            if (mSslSocketFactory != null) {
                Logger.w("Setting factory in stream request");
                ((HttpsURLConnection) mConnection).setSSLSocketFactory(mSslSocketFactory);
            }

            response = buildResponse(mConnection);
        } catch (MalformedURLException e) {
            throw new HttpException("URL is malformed: " + e.getLocalizedMessage());
        } catch (ProtocolException e) {
            throw new HttpException("Http method not allowed: " + e.getLocalizedMessage());
        } catch (IOException e) {
            throw new HttpException("Something happened while retrieving data: " + e.getLocalizedMessage());
        }
        return response;
    }

    private void addHeaders(HttpURLConnection connection) {
        for (Map.Entry<String, String> entry : mHeaders.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
    }

    private static HttpStreamResponse buildResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < 300 && connection.getInputStream() != null) {
            BufferedReader mResponseBufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            return new HttpStreamResponseImpl(responseCode, mResponseBufferedReader);
        }

        return new HttpStreamResponseImpl(responseCode);
    }
}
