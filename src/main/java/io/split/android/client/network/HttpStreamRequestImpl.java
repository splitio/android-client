package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import io.split.android.client.utils.logger.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class HttpStreamRequestImpl implements HttpStreamRequest {

    private static final int STREAMING_READ_TIMEOUT_IN_MILLISECONDS = 80000;
    private final URI mUri;
    private final Map<String, String> mHeaders;
    private final SSLSocketFactory mSslSocketFactory;
    private HttpURLConnection mConnection;
    private BufferedReader mResponseBufferedReader;
    private InputStream mResponseInputStream;

    HttpStreamRequestImpl(@NonNull URI uri,
                                  @NonNull Map<String, String> headers,
                                  @Nullable SSLSocketFactory sslSocketFactory) {
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
        if (mResponseInputStream != null) {
            try {
                mResponseInputStream.close();
            } catch (IOException e) {
                Logger.d("Unknown error closing streaming connection: " + e.getLocalizedMessage());
            } catch (Exception e) {
                Logger.d("Unknown error closing stream: " + e.getLocalizedMessage());
            }
        }
        if (mResponseBufferedReader != null) {
            try {
                mResponseBufferedReader.close();
            } catch (IOException e) {
                Logger.d("Buffer already closed");
            } catch (Exception e) {
                Logger.d("Unknown error closing buffer: " + e.getLocalizedMessage());
            }
        }
        mConnection.disconnect();
        Logger.d("Streaming connection closed");
    }

    private HttpStreamResponse getRequest() throws HttpException {
        URL url;
        HttpStreamResponse response;
        try {
            url = mUri.toURL();
            mConnection = (HttpURLConnection) url.openConnection();

            mConnection.setReadTimeout(STREAMING_READ_TIMEOUT_IN_MILLISECONDS);

            addHeaders(mConnection);

            if (mSslSocketFactory != null) {
                Logger.d("Setting  SSL socket factory in stream request");
                if (mConnection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) mConnection).setSSLSocketFactory(mSslSocketFactory);
                } else {
                    Logger.d("Failed to set SSL socket factory in stream request. Connection is not SSL");
                }
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

    private void addHeaders(HttpURLConnection request) {
        if (mHeaders == null) {
            return;
        }

        for (Map.Entry<String, String> entry : mHeaders.entrySet()) {
            request.setRequestProperty(entry.getKey(), entry.getValue());
        }
    }

    private HttpStreamResponse buildResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {
            mResponseInputStream = connection.getInputStream();
            if (mResponseInputStream != null) {
                mResponseBufferedReader = new BufferedReader(new InputStreamReader(mResponseInputStream));

                return new HttpStreamResponseImpl(responseCode, mResponseBufferedReader);
            }
        }
        return new HttpStreamResponseImpl(responseCode);
    }
}
