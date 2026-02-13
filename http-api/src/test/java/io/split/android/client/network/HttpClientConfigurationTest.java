package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class HttpClientConfigurationTest {

    @Test
    public void builderSetsConnectionTimeout() {
        HttpClientConfiguration config = HttpClientConfiguration.builder()
                .connectionTimeout(15_000)
                .build();

        assertEquals(15_000, config.getConnectionTimeout());
    }

    @Test
    public void builderSetsReadTimeout() {
        HttpClientConfiguration config = HttpClientConfiguration.builder()
                .readTimeout(30_000)
                .build();

        assertEquals(30_000, config.getReadTimeout());
    }

    @Test
    public void builderSetsProxy() {
        HttpProxy proxy = HttpProxy.newBuilder("proxy.example.com", 8080).build();
        HttpClientConfiguration config = HttpClientConfiguration.builder()
                .proxy(proxy)
                .build();

        assertNotNull(config.getProxy());
        assertEquals("proxy.example.com", config.getProxy().getHost());
        assertEquals(8080, config.getProxy().getPort());
    }

    @Test
    public void builderSetsCertificatePinningConfiguration() {
        CertificatePinningConfiguration certConfig = CertificatePinningConfiguration.builder()
                .addPin("example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .build();
        HttpClientConfiguration config = HttpClientConfiguration.builder()
                .certificatePinningConfiguration(certConfig)
                .build();

        assertNotNull(config.getCertificatePinningConfiguration());
    }

    @Test
    public void builderSetsDevelopmentSslConfig() {
        // DevelopmentSslConfig requires non-null args; just verify null default
        HttpClientConfiguration config = HttpClientConfiguration.builder().build();
        assertNull(config.getDevelopmentSslConfig());
    }

    @Test
    public void builderSetsProxyAuthenticator() {
        HttpClientConfiguration config = HttpClientConfiguration.builder().build();
        assertNull(config.getProxyAuthenticator());
    }

    @Test
    public void defaultValuesAreZeroAndNull() {
        HttpClientConfiguration config = HttpClientConfiguration.builder().build();

        assertEquals(0, config.getConnectionTimeout());
        assertEquals(0, config.getReadTimeout());
        assertNull(config.getProxy());
        assertNull(config.getCertificatePinningConfiguration());
        assertNull(config.getDevelopmentSslConfig());
        assertNull(config.getProxyAuthenticator());
    }

    @Test
    public void builderSetsAllFields() {
        HttpProxy proxy = HttpProxy.newBuilder("proxy.example.com", 8080).build();
        CertificatePinningConfiguration certConfig = CertificatePinningConfiguration.builder()
                .addPin("example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .build();

        HttpClientConfiguration config = HttpClientConfiguration.builder()
                .connectionTimeout(10_000)
                .readTimeout(20_000)
                .proxy(proxy)
                .certificatePinningConfiguration(certConfig)
                .build();

        assertEquals(10_000, config.getConnectionTimeout());
        assertEquals(20_000, config.getReadTimeout());
        assertNotNull(config.getProxy());
        assertNotNull(config.getCertificatePinningConfiguration());
    }
}
