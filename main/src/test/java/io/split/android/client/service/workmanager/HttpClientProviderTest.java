package io.split.android.client.service.workmanager;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mockStatic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import io.split.android.client.dtos.HttpProxyDto;
import io.split.android.client.network.CertificatePinningConfiguration;
import io.split.android.client.network.CertificatePinningConfigurationProvider;
import io.split.android.client.network.HttpClient;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.utils.HttpProxySerializer;

@RunWith(MockitoJUnitRunner.class)
public class HttpClientProviderTest {

    @Mock
    private SplitRoomDatabase mockDatabase;
    
    @Mock
    private GeneralInfoStorage mockGeneralInfoStorage;
    
    @Mock
    private SplitCipher mockSplitCipher;
    
    @Mock
    private CertificatePinningConfiguration mockCertPinningConfig;
    
    @Mock
    private HttpProxyDto mockHttpProxyDto;

    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_CERT_PINNING_CONFIG = "{\"pins\":[]}";

    @Test
    public void shouldBuildHttpClientWithNullCertificatePinningConfig() {
        HttpClient result = buildHttpClientWithMocks(null, false, null);
        assertNotNull("HttpClient should not be null", result);
    }

    @Test
    public void shouldBuildHttpClientWithValidCertificatePinningConfig() {
        HttpClient result = buildHttpClientWithCertPinningMocks(false);
        assertNotNull("HttpClient should not be null", result);
    }

    @Test
    public void shouldBuildHttpClientWithValidProxyConfig() {
        mockHttpProxyDto.host = "proxy.example.com";
        mockHttpProxyDto.port = 8080;
        
        HttpClient result = buildHttpClientWithMocks(null, true, mockHttpProxyDto);
        assertNotNull("HttpClient should not be null", result);
    }

    @Test
    public void shouldBuildHttpClientWhenProxyConfigProvidedButDtoIsNull() {
        HttpClient result = buildHttpClientWithMocks(null, true, null);
        assertNotNull("HttpClient should not be null", result);
    }

    @Test
    public void shouldBuildHttpClientWithProxyBasicAuth() {
        mockHttpProxyDto.host = "proxy.example.com";
        mockHttpProxyDto.port = 8080;
        mockHttpProxyDto.username = "testuser";
        mockHttpProxyDto.password = "testpass";
        
        HttpClient result = buildHttpClientWithMocks(null, true, mockHttpProxyDto);
        assertNotNull("HttpClient should not be null", result);
    }

    @Test
    public void shouldBuildHttpClientWithProxyBearerToken() {
        mockHttpProxyDto.host = "proxy.example.com";
        mockHttpProxyDto.port = 8080;
        mockHttpProxyDto.bearerToken = "test-bearer-token";
        
        HttpClient result = buildHttpClientWithMocks(null, true, mockHttpProxyDto);
        assertNotNull("HttpClient should not be null", result);
    }

    @Test
    public void shouldBuildHttpClientWithProxyMtlsAuth() {
        mockHttpProxyDto.host = "proxy.example.com";
        mockHttpProxyDto.port = 8080;
        mockHttpProxyDto.clientCert = "-----BEGIN CERTIFICATE-----\nMIIC...\n-----END CERTIFICATE-----";
        mockHttpProxyDto.clientKey = "-----BEGIN PRIVATE KEY-----\nMIIE...\n-----END PRIVATE KEY-----";
        
        HttpClient result = buildHttpClientWithMocks(null, true, mockHttpProxyDto);
        assertNotNull("HttpClient should not be null", result);
    }

    @Test
    public void shouldBuildHttpClientWhenProxyHostIsNull() {
        mockHttpProxyDto.host = null;
        mockHttpProxyDto.port = 8080;
        
        HttpClient result = buildHttpClientWithMocks(null, true, mockHttpProxyDto);
        assertNotNull("HttpClient should not be null", result);
    }

    @Test
    public void shouldBuildHttpClientWithEmptyCertificatePinningConfig() {
        HttpClient result = buildHttpClientWithMocks("", false, null);
        assertNotNull("HttpClient should not be null", result);
    }

    @Test
    public void shouldBuildHttpClientWithProxyCaCert() {
        mockHttpProxyDto.host = "proxy.example.com";
        mockHttpProxyDto.port = 8080;
        mockHttpProxyDto.caCert = "-----BEGIN CERTIFICATE-----\nMIIC...\n-----END CERTIFICATE-----";
        
        HttpClient result = buildHttpClientWithMocks(null, true, mockHttpProxyDto);
        assertNotNull("HttpClient should not be null", result);
    }

    private void setupCommonMocks(MockedStatic<StorageFactory> storageFactoryMock,
                                  MockedStatic<SplitCipherFactory> cipherFactoryMock,
                                  MockedStatic<HttpProxySerializer> serializerMock,
                                  HttpProxyDto proxyDto) {
        cipherFactoryMock.when(() -> SplitCipherFactory.create(TEST_API_KEY, true))
                .thenReturn(mockSplitCipher);
        storageFactoryMock.when(() -> StorageFactory.getGeneralInfoStorage(mockDatabase, mockSplitCipher))
                .thenReturn(mockGeneralInfoStorage);
        serializerMock.when(() -> HttpProxySerializer.deserialize(mockGeneralInfoStorage))
                .thenReturn(proxyDto);
    }

    private HttpClient buildHttpClientWithMocks(String certPinningConfig, boolean usingProxy, HttpProxyDto proxyDto) {
        try (MockedStatic<StorageFactory> storageFactoryMock = mockStatic(StorageFactory.class);
             MockedStatic<SplitCipherFactory> cipherFactoryMock = mockStatic(SplitCipherFactory.class);
             MockedStatic<HttpProxySerializer> serializerMock = mockStatic(HttpProxySerializer.class)) {
            
            setupCommonMocks(storageFactoryMock, cipherFactoryMock, serializerMock, proxyDto);
            
            return HttpClientProvider.buildHttpClient(
                TEST_API_KEY, 
                certPinningConfig, 
                usingProxy,
                mockDatabase
            );
        }
    }

    private HttpClient buildHttpClientWithCertPinningMocks(boolean usingProxy) {
        try (MockedStatic<StorageFactory> storageFactoryMock = mockStatic(StorageFactory.class);
             MockedStatic<SplitCipherFactory> cipherFactoryMock = mockStatic(SplitCipherFactory.class);
             MockedStatic<HttpProxySerializer> serializerMock = mockStatic(HttpProxySerializer.class);
             MockedStatic<CertificatePinningConfigurationProvider> certProviderMock = mockStatic(CertificatePinningConfigurationProvider.class)) {
            
            setupCommonMocks(storageFactoryMock, cipherFactoryMock, serializerMock, null);
            certProviderMock.when(() -> CertificatePinningConfigurationProvider.getCertificatePinningConfiguration(HttpClientProviderTest.TEST_CERT_PINNING_CONFIG))
                    .thenReturn(mockCertPinningConfig);
            
            HttpClient result = HttpClientProvider.buildHttpClient(
                TEST_API_KEY,
                    HttpClientProviderTest.TEST_CERT_PINNING_CONFIG,
                usingProxy,
                mockDatabase
            );
            
            certProviderMock.verify(() -> CertificatePinningConfigurationProvider.getCertificatePinningConfiguration(HttpClientProviderTest.TEST_CERT_PINNING_CONFIG));
            return result;
        }
    }
}
