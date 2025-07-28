package io.split.android.client.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.split.android.client.dtos.HttpProxyDto;
import io.split.android.client.network.BasicCredentialsProvider;
import io.split.android.client.network.HttpProxy;
import io.split.android.client.network.ProxyCredentialsProvider;
import io.split.android.client.storage.general.GeneralInfoStorage;

public class HttpProxySerializerTest {

    private HttpProxy mHttpProxy;
    private GeneralInfoStorage mGeneralInfoStorage;
    private final String TEST_HOST = "proxy.example.com";
    private final int TEST_PORT = 8080;
    private final String TEST_USERNAME = "testuser";
    private final String TEST_PASSWORD = "testpass";
    private final String TEST_CLIENT_CERT = "-----BEGIN CERTIFICATE-----\nMIICertificateContent\n-----END CERTIFICATE-----";
    private final String TEST_CLIENT_KEY = "-----BEGIN PRIVATE KEY-----\nMIIKeyContent\n-----END PRIVATE KEY-----";
    private final String TEST_CA_CERT = "-----BEGIN CA CERTIFICATE-----\nMIICACertContent\n-----END CA CERTIFICATE-----";

    @Before
    public void setUp() {
        mGeneralInfoStorage = mock(GeneralInfoStorage.class);

        // Create input streams from test strings
        InputStream clientCertStream = new ByteArrayInputStream(TEST_CLIENT_CERT.getBytes(StandardCharsets.UTF_8));
        InputStream clientKeyStream = new ByteArrayInputStream(TEST_CLIENT_KEY.getBytes(StandardCharsets.UTF_8));
        InputStream caCertStream = new ByteArrayInputStream(TEST_CA_CERT.getBytes(StandardCharsets.UTF_8));
        
        // Mock the credentials provider
        ProxyCredentialsProvider credentialsProvider = new BasicCredentialsProvider() {
            @Override
            public String getUsername() {
                return TEST_USERNAME;
            }

            @Override
            public String getPassword() {
                return TEST_PASSWORD;
            }
        };
        
        // Create the HttpProxy object
        mHttpProxy = HttpProxy.newBuilder(TEST_HOST, TEST_PORT)
                .basicAuth(TEST_USERNAME, TEST_PASSWORD)
                .mtlsAuth(clientCertStream, clientKeyStream)
                .proxyCacert(caCertStream)
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Test
    public void serializeHttpProxyWorks() {
        // Serialize the HttpProxy object
        String json = HttpProxySerializer.serialize(mHttpProxy);
        when(mGeneralInfoStorage.getProxyConfig()).thenReturn(json);
        
        // Verify the serialization result
        assertNotNull("Serialized JSON should not be null", json);
        
        // Deserialize back to HttpProxyDto
        HttpProxyDto dto = HttpProxySerializer.deserialize(mGeneralInfoStorage);
        
        // Verify the deserialized object
        assertNotNull("Deserialized DTO should not be null", dto);
        assertEquals("Host should match", TEST_HOST, dto.host);
        assertEquals("Port should match", TEST_PORT, dto.port);
        assertEquals("Username should match", TEST_USERNAME, dto.username);
        assertEquals("Password should match", TEST_PASSWORD, dto.password);
        assertEquals("Client cert should match", TEST_CLIENT_CERT, dto.clientCert);
        assertEquals("Client key should match", TEST_CLIENT_KEY, dto.clientKey);
        assertEquals("CA cert should match", TEST_CA_CERT, dto.caCert);
        assertNull("Bearer token should be null", dto.bearerToken);
    }

    @Test
    public void testSerializeNullHttpProxy() {
        String json = HttpProxySerializer.serialize(null);
        assertNull("Serializing null should return null", json);
    }

    @Test
    public void deserializeNullJsonReturnsNull() {
        when(mGeneralInfoStorage.getProxyConfig()).thenReturn(null);
        HttpProxyDto dto = HttpProxySerializer.deserialize(mGeneralInfoStorage);
        assertNull("Deserializing null should return null", dto);
    }

    @Test
    public void deserializeEmptyJsonReturnsNull() {
        when(mGeneralInfoStorage.getProxyConfig()).thenReturn("");
        HttpProxyDto dto = HttpProxySerializer.deserialize(mGeneralInfoStorage);
        assertNull("Deserializing empty string should return null", dto);
    }

    @Test
    public void deserializeInvalidJsonReturnsNull() {
        when(mGeneralInfoStorage.getProxyConfig()).thenReturn("{ invalid json }");
        HttpProxyDto dto = HttpProxySerializer.deserialize(mGeneralInfoStorage);
        assertNull("Deserializing invalid JSON should return null", dto);
    }
}
