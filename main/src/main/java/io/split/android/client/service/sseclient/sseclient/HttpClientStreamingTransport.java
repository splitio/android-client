package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpStreamRequest;
import io.split.android.client.network.HttpStreamResponse;
import io.split.android.client.service.sseclient.spi.StreamingTransport;

/**
 * Adapter that implements StreamingTransport using HttpClient.
 */
public class HttpClientStreamingTransport implements StreamingTransport {

    private final HttpClient mHttpClient;

    public HttpClientStreamingTransport(@NonNull HttpClient httpClient) {
        mHttpClient = httpClient;
    }

    @NonNull
    @Override
    public StreamingConnection connect(@NonNull URI uri) {
        return new HttpClientStreamingConnection(mHttpClient.streamRequest(uri));
    }

    private static class HttpClientStreamingConnection implements StreamingConnection {
        private final HttpStreamRequest mRequest;

        HttpClientStreamingConnection(HttpStreamRequest request) {
            mRequest = request;
        }

        @NonNull
        @Override
        public StreamingResponse execute() throws StreamingTransportException {
            try {
                HttpStreamResponse response = mRequest.execute();
                return new HttpClientStreamingResponse(response);
            } catch (HttpException e) {
                throw new StreamingTransportException(e.getMessage(), e, e.getStatusCode());
            } catch (IOException e) {
                throw new StreamingTransportException(e.getMessage(), e);
            }
        }

        @Override
        public void close() {
            mRequest.close();
        }
    }

    private static class HttpClientStreamingResponse implements StreamingResponse {
        private final HttpStreamResponse mResponse;

        HttpClientStreamingResponse(HttpStreamResponse response) {
            mResponse = response;
        }

        @Override
        public boolean isSuccess() {
            return mResponse.isSuccess();
        }

        @Override
        public int getHttpStatus() {
            return mResponse.getHttpStatus();
        }

        @Override
        public boolean isClientRelatedError() {
            return mResponse.isClientRelatedError();
        }

        @Nullable
        @Override
        public BufferedReader getBufferedReader() {
            return mResponse.getBufferedReader();
        }

        @Override
        public void close() throws IOException {
            mResponse.close();
        }
    }
}
