package io.split.android.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;

import io.split.android.client.network.AuthenticatedRequest;
import io.split.android.client.network.CertificatePinningConfiguration;
import io.split.android.client.network.DevelopmentSslConfig;
import io.split.android.client.network.HttpClientConfiguration;
import io.split.android.client.network.HttpProxy;
import io.split.android.client.network.SplitAuthenticator;

public class SplitFactoryImplConfigMappingTest {

    @Test
    public void buildHttpClientConfigurationMapsAllFields() {
        SplitClientConfig splitConfig = mock(SplitClientConfig.class);
        HttpProxy proxy = HttpProxy.newBuilder("proxy.example.com", 8080).build();
        CertificatePinningConfiguration pinConfig = mock(CertificatePinningConfiguration.class);
        DevelopmentSslConfig devSsl = mock(DevelopmentSslConfig.class);
        SplitAuthenticator authenticator = new SplitAuthenticator() {
            @Nullable
            @Override
            public AuthenticatedRequest authenticate(@NonNull AuthenticatedRequest request) {
                return request;
            }
        };

        when(splitConfig.connectionTimeout()).thenReturn(5000);
        when(splitConfig.readTimeout()).thenReturn(10000);
        when(splitConfig.proxy()).thenReturn(proxy);
        when(splitConfig.certificatePinningConfiguration()).thenReturn(pinConfig);
        when(splitConfig.developmentSslConfig()).thenReturn(devSsl);
        when(splitConfig.authenticator()).thenReturn(authenticator);

        HttpClientConfiguration result = SplitFactoryImpl.buildHttpClientConfiguration(splitConfig);

        assertEquals(5000, result.getConnectionTimeout());
        assertEquals(10000, result.getReadTimeout());
        assertNotNull(result.getProxy());
        assertEquals("proxy.example.com", result.getProxy().getHost());
        assertEquals(8080, result.getProxy().getPort());
        assertSame(pinConfig, result.getCertificatePinningConfiguration());
        assertSame(devSsl, result.getDevelopmentSslConfig());
        assertSame(authenticator, result.getProxyAuthenticator());
    }

    @Test
    public void buildHttpClientConfigurationWithNullOptionals() {
        SplitClientConfig splitConfig = mock(SplitClientConfig.class);

        when(splitConfig.connectionTimeout()).thenReturn(3000);
        when(splitConfig.readTimeout()).thenReturn(6000);
        when(splitConfig.proxy()).thenReturn(null);
        when(splitConfig.certificatePinningConfiguration()).thenReturn(null);
        when(splitConfig.developmentSslConfig()).thenReturn(null);
        when(splitConfig.authenticator()).thenReturn(null);

        HttpClientConfiguration result = SplitFactoryImpl.buildHttpClientConfiguration(splitConfig);

        assertEquals(3000, result.getConnectionTimeout());
        assertEquals(6000, result.getReadTimeout());
        assertNull(result.getProxy());
        assertNull(result.getCertificatePinningConfiguration());
        assertNull(result.getDevelopmentSslConfig());
        assertNull(result.getProxyAuthenticator());
    }

    @Test
    public void buildHttpClientConfigurationWithZeroTimeouts() {
        SplitClientConfig splitConfig = mock(SplitClientConfig.class);

        when(splitConfig.connectionTimeout()).thenReturn(0);
        when(splitConfig.readTimeout()).thenReturn(0);
        when(splitConfig.proxy()).thenReturn(null);
        when(splitConfig.certificatePinningConfiguration()).thenReturn(null);
        when(splitConfig.developmentSslConfig()).thenReturn(null);
        when(splitConfig.authenticator()).thenReturn(null);

        HttpClientConfiguration result = SplitFactoryImpl.buildHttpClientConfiguration(splitConfig);

        assertEquals(0, result.getConnectionTimeout());
        assertEquals(0, result.getReadTimeout());
    }

    @Test
    public void buildHttpClientConfigurationWithOnlyProxy() {
        SplitClientConfig splitConfig = mock(SplitClientConfig.class);
        HttpProxy proxy = HttpProxy.newBuilder("myproxy.local", 3128).build();

        when(splitConfig.connectionTimeout()).thenReturn(15000);
        when(splitConfig.readTimeout()).thenReturn(15000);
        when(splitConfig.proxy()).thenReturn(proxy);
        when(splitConfig.certificatePinningConfiguration()).thenReturn(null);
        when(splitConfig.developmentSslConfig()).thenReturn(null);
        when(splitConfig.authenticator()).thenReturn(null);

        HttpClientConfiguration result = SplitFactoryImpl.buildHttpClientConfiguration(splitConfig);

        assertEquals(15000, result.getConnectionTimeout());
        assertEquals(15000, result.getReadTimeout());
        assertNotNull(result.getProxy());
        assertEquals("myproxy.local", result.getProxy().getHost());
        assertEquals(3128, result.getProxy().getPort());
        assertNull(result.getCertificatePinningConfiguration());
        assertNull(result.getDevelopmentSslConfig());
        assertNull(result.getProxyAuthenticator());
    }
}
