package io.split.android.client.network;

import com.google.common.base.Strings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class HttpClientImpl implements HttpClient {
    private static final String BASIC_AUTHENTICATION_HEADER = "Authorization";
    private static final long STREAMING_CONNECTION_TIMEOUT_IN_SECONDS = 80;
    private OkHttpClient mOkHttpClient;
    private OkHttpClient mOkHttpClientStreaming;
    private Map<String, String> mHeaders;

    private HttpClientImpl(OkHttpClient okHttpClient, OkHttpClient okHttpClientStreaming) {
        mHeaders = new HashMap<>();
        mOkHttpClient = okHttpClient;
        mOkHttpClientStreaming = okHttpClientStreaming;
    }

    @Override
    public HttpRequest request(URI uri, HttpMethod requestMethod) {
        return request(uri, requestMethod, null);
    }

    @Override
    public HttpRequest request(URI uri, HttpMethod requestMethod, String body) {
        return new HttpRequestImpl(mOkHttpClient, uri, requestMethod, body, mHeaders);
    }

    @Override
    public HttpStreamRequest streamRequest(URI uri) {
        return new HttpStreamRequestImpl(mOkHttpClientStreaming, uri, mHeaders);
    }

    @Override
    public void setHeader(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException(String.format("Invalid value for header %s: %s", name, value));
        }
        mHeaders.put(name, value);
    }

    @Override
    public void addHeaders(Map<String, String> headers) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            setHeader(header.getKey(), header.getValue());
        }
    }

    @Override
    public void close() {
        mOkHttpClient.connectionPool().evictAll();
        mOkHttpClientStreaming.connectionPool().evictAll();
    }

    public static class Builder {
        private Authenticator mProxyAuthenticator;
        private HttpProxy mProxy;

        public Builder setProxy(HttpProxy proxy) {
            mProxy = proxy;
            return this;
        }

        public Builder setProxyAuthenticator(Authenticator authenticator) {
            mProxyAuthenticator = authenticator;
            return this;
        }

        public HttpClient build() {
            Proxy proxy = null;
            Authenticator authenticator = null;
            if(mProxy != null) {
                proxy = createProxy(mProxy);
                if(mProxyAuthenticator != null) {
                    authenticator = mProxyAuthenticator;
                } else if(!Strings.isNullOrEmpty(mProxy.getUsername())) {
                    authenticator = createBasicAuthenticator(mProxy.getUsername(), mProxy.getPassword());
                }
            }

            // Avoiding newBuilder on purpose to use different thread pool and resources
            return new HttpClientImpl(
                    createOkHttpClient(proxy, mProxyAuthenticator, null),
                    createOkHttpClient(proxy, mProxyAuthenticator,STREAMING_CONNECTION_TIMEOUT_IN_SECONDS)
            );
        }

        private OkHttpClient createOkHttpClient(Proxy proxy,
                                                Authenticator authenticator,
                                                Long readTimeout) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();

            if(proxy != null) {
                builder.proxy(proxy);
            }

            if(authenticator != null) {
                builder.proxyAuthenticator(authenticator);
            }

            if(readTimeout != null) {
                builder.readTimeout(readTimeout, TimeUnit.SECONDS);
            }

            return builder.build();
        }

        private Proxy createProxy(HttpProxy proxy) {
            if(proxy == null) {
                return null;
            }
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort()));
        }

        private Authenticator createBasicAuthenticator(String username, String password) {
            return new Authenticator() {
                @Nullable
                @Override
                public Request authenticate(@Nullable Route route, @NotNull Response response) throws IOException {
                    String credential = Credentials.basic(username, password);
                    return response.request().newBuilder().header(BASIC_AUTHENTICATION_HEADER, credential).build();
                }
            };
        }
    }
}
