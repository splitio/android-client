package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;

public class HttpClientImplBuilderConfigurationTest {

    @Test
    public void configurationAppliesAllValuesWhenBuilderHasDefaults() {
        HttpProxy proxy = HttpProxy.newBuilder("proxy.example.com", 8080).build();
        SplitAuthenticator authenticator = new SplitAuthenticator() {
            @Nullable
            @Override
            public AuthenticatedRequest authenticate(@NonNull AuthenticatedRequest request) {
                return request;
            }
        };
        CertificatePinningConfiguration pinConfig = mock(CertificatePinningConfiguration.class);
        DevelopmentSslConfig devSsl = mock(DevelopmentSslConfig.class);

        HttpClientConfiguration config = HttpClientConfiguration.builder()
                .connectionTimeout(5000)
                .readTimeout(10000)
                .proxy(proxy)
                .proxyAuthenticator(authenticator)
                .certificatePinningConfiguration(pinConfig)
                .developmentSslConfig(devSsl)
                .build();

        HttpClientImpl client = (HttpClientImpl) new HttpClientImpl.Builder()
                .setConfiguration(config)
                .build();

        assertEquals(5000, client.getConnectionTimeout());
        assertEquals(10000, client.getReadTimeout());
        assertNotNull(client.getHttpProxy());
        assertEquals("proxy.example.com", client.getHttpProxy().getHost());
        assertEquals(8080, client.getHttpProxy().getPort());
        assertNotNull(client.getProxyAuthenticator());
        assertNotNull(client.getCertificateChecker());
        assertNotNull(client.getDevelopmentSslConfig());
    }

    @Test
    public void builderValuesTakePrecedenceOverConfiguration() {
        HttpProxy configProxy = HttpProxy.newBuilder("config.proxy.com", 9090).build();
        HttpProxy builderProxy = HttpProxy.newBuilder("builder.proxy.com", 7070).build();

        HttpClientConfiguration config = HttpClientConfiguration.builder()
                .connectionTimeout(5000)
                .readTimeout(10000)
                .proxy(configProxy)
                .build();

        HttpClientImpl client = (HttpClientImpl) new HttpClientImpl.Builder()
                .setConnectionTimeout(1000)
                .setReadTimeout(2000)
                .setProxy(builderProxy)
                .setConfiguration(config)
                .build();

        // Builder values should win
        assertEquals(1000, client.getConnectionTimeout());
        assertEquals(2000, client.getReadTimeout());
        assertEquals("builder.proxy.com", client.getHttpProxy().getHost());
        assertEquals(7070, client.getHttpProxy().getPort());
    }

    @Test
    public void configurationWithNullOptionalFieldsDoesNotOverrideBuilderDefaults() {
        HttpClientConfiguration config = HttpClientConfiguration.builder()
                .connectionTimeout(3000)
                .readTimeout(6000)
                .build();

        HttpClientImpl client = (HttpClientImpl) new HttpClientImpl.Builder()
                .setConfiguration(config)
                .build();

        assertEquals(3000, client.getConnectionTimeout());
        assertEquals(6000, client.getReadTimeout());
        assertNull(client.getHttpProxy());
        assertNull(client.getProxyAuthenticator());
        assertNull(client.getCertificateChecker());
        assertNull(client.getDevelopmentSslConfig());
    }

    @Test
    public void buildWithoutConfigurationUsesBuilderDefaults() {
        HttpClientImpl client = (HttpClientImpl) new HttpClientImpl.Builder()
                .setConnectionTimeout(4000)
                .setReadTimeout(8000)
                .build();

        assertEquals(4000, client.getConnectionTimeout());
        assertEquals(8000, client.getReadTimeout());
        assertNull(client.getHttpProxy());
        assertNull(client.getProxyAuthenticator());
        assertNull(client.getCertificateChecker());
        assertNull(client.getDevelopmentSslConfig());
    }

    @Test
    public void builderAuthenticatorTakesPrecedenceOverConfiguration() {
        SplitAuthenticator configAuth = new SplitAuthenticator() {
            @Nullable
            @Override
            public AuthenticatedRequest authenticate(@NonNull AuthenticatedRequest request) {
                request.setHeader("Source", "config");
                return request;
            }
        };
        SplitAuthenticator builderAuth = new SplitAuthenticator() {
            @Nullable
            @Override
            public AuthenticatedRequest authenticate(@NonNull AuthenticatedRequest request) {
                request.setHeader("Source", "builder");
                return request;
            }
        };

        HttpProxy proxy = HttpProxy.newBuilder("proxy.example.com", 8080).build();

        HttpClientConfiguration config = HttpClientConfiguration.builder()
                .proxy(proxy)
                .proxyAuthenticator(configAuth)
                .build();

        HttpClientImpl client = (HttpClientImpl) new HttpClientImpl.Builder()
                .setProxy(proxy)
                .setProxyAuthenticator(builderAuth)
                .setConfiguration(config)
                .build();

        // Builder authenticator should win — proxy authenticator should not be null
        assertNotNull(client.getProxyAuthenticator());
    }

    @Test
    public void configurationWithNullProxyDoesNotSetProxy() {
        HttpClientConfiguration config = HttpClientConfiguration.builder()
                .connectionTimeout(1000)
                .readTimeout(2000)
                .proxy(null)
                .build();

        HttpClientImpl client = (HttpClientImpl) new HttpClientImpl.Builder()
                .setConfiguration(config)
                .build();

        assertNull(client.getHttpProxy());
    }
}
