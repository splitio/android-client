package io.split.android.client.storage.general;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.split.android.client.dtos.HttpProxyDto;
import io.split.android.client.network.HttpProxy;
import io.split.android.client.network.ProxyCredentialsProvider;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.GeneralInfoDao;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.utils.HttpProxySerializer;

public class GeneralInfoStorageImplTest {

    private GeneralInfoDao mGeneralInfoDao;
    private SplitCipher mAlwaysEncryptedSplitCipher;
    private GeneralInfoStorageImpl mGeneralInfoStorage;

    @Before
    public void setUp() {
        mAlwaysEncryptedSplitCipher = mock(SplitCipher.class);
        when(mAlwaysEncryptedSplitCipher.encrypt(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return "encrypted_" + invocation.getArgument(0);
            }
        });
        when(mAlwaysEncryptedSplitCipher.decrypt(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return "decrypted_" + invocation.getArgument(0);
            }
        });

        mGeneralInfoDao = mock(GeneralInfoDao.class);
        mGeneralInfoStorage = new GeneralInfoStorageImpl(mGeneralInfoDao, mAlwaysEncryptedSplitCipher);
    }

    @Test
    public void setSplitsUpdateTimestampSetsValueOnDao() {
        mGeneralInfoStorage.setSplitsUpdateTimestamp(123L);

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals("splitsUpdateTimestamp") && entity.getLongValue() == 123L));
    }

    @Test
    public void getSplitsUpdateTimestampGetsValueFromDao() {
        when(mGeneralInfoDao.getByName("splitsUpdateTimestamp")).thenReturn(new GeneralInfoEntity("splitsUpdateTimestamp", 123L));
        long splitsUpdateTimestamp = mGeneralInfoStorage.getSplitsUpdateTimestamp();

        assertEquals(123L, splitsUpdateTimestamp);
        verify(mGeneralInfoDao).getByName("splitsUpdateTimestamp");
    }

    @Test
    public void setFlagsChangeNumberSetsValueOnDao() {
        mGeneralInfoStorage.setFlagsChangeNumber(123L);

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals("splitChangesChangeNumber") && entity.getLongValue() == 123L));
    }

    @Test
    public void getFlagsChangeNumberGetsValueFromDao() {
        when(mGeneralInfoDao.getByName("splitChangesChangeNumber")).thenReturn(new GeneralInfoEntity("splitChangesChangeNumber", 123L));
        long changeNumber = mGeneralInfoStorage.getFlagsChangeNumber();

        assertEquals(123L, changeNumber);
        verify(mGeneralInfoDao).getByName("splitChangesChangeNumber");
    }

    @Test
    public void getSplitsFilterQueryStringGetsValueFromDao() {
        when(mGeneralInfoDao.getByName("splitsFilterQueryString")).thenReturn(new GeneralInfoEntity("splitsFilterQueryString", "queryString"));
        String splitsFilterQueryString = mGeneralInfoStorage.getSplitsFilterQueryString();

        assertEquals("queryString", splitsFilterQueryString);
        verify(mGeneralInfoDao).getByName("splitsFilterQueryString");
    }

    @Test
    public void setSplitsFilterQueryStringSetsValueOnDao() {
        mGeneralInfoStorage.setSplitsFilterQueryString("queryString");

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals("splitsFilterQueryString") && entity.getStringValue().equals("queryString")));
    }

    @Test
    public void getDatabaseEncryptionModeGetsValueFromDao() {
        when(mGeneralInfoDao.getByName("databaseEncryptionMode")).thenReturn(new GeneralInfoEntity("databaseEncryptionMode", "value"));
        String databaseEncryptionMode = mGeneralInfoStorage.getDatabaseEncryptionMode();

        assertEquals("value", databaseEncryptionMode);
        verify(mGeneralInfoDao).getByName("databaseEncryptionMode");
    }

    @Test
    public void setDatabaseEncryptionModeSetsValueOnDao() {
        mGeneralInfoStorage.setDatabaseEncryptionMode("value");

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals("databaseEncryptionMode") && entity.getStringValue().equals("value")));
    }

    @Test
    public void getFlagsSpecGetsValueFromDao() {
        when(mGeneralInfoDao.getByName("flagsSpec")).thenReturn(new GeneralInfoEntity("flagsSpec", "value"));
        String flagsSpec = mGeneralInfoStorage.getFlagsSpec();

        assertEquals("value", flagsSpec);
        verify(mGeneralInfoDao).getByName("flagsSpec");
    }

    @Test
    public void setFlagsSpecSetsValueOnDao() {
        mGeneralInfoStorage.setFlagsSpec("value");

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals("flagsSpec") && entity.getStringValue().equals("value")));
    }

    @Test
    public void getRolloutCacheLastClearTimestampGetsValueFromDao() {
        when(mGeneralInfoDao.getByName("rolloutCacheLastClearTimestamp")).thenReturn(new GeneralInfoEntity("rolloutCacheLastClearTimestamp", 123L));
        long rolloutCacheLastClearTimestamp = mGeneralInfoStorage.getRolloutCacheLastClearTimestamp();

        assertEquals(123L, rolloutCacheLastClearTimestamp);
        verify(mGeneralInfoDao).getByName("rolloutCacheLastClearTimestamp");
    }

    @Test
    public void setRolloutCacheLastClearTimestampSetsValueOnDao() {
        mGeneralInfoStorage.setRolloutCacheLastClearTimestamp(123L);

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals("rolloutCacheLastClearTimestamp") && entity.getLongValue() == 123L));
    }

    @Test
    public void getFlagsChangeNumberReturnsMinusOneIfEntityIsNull() {
        when(mGeneralInfoDao.getByName("splitChangesChangeNumber")).thenReturn(null);
        long changeNumber = mGeneralInfoStorage.getFlagsChangeNumber();

        assertEquals(-1L, changeNumber);
    }

    @Test
    public void getSplitsUpdateTimestampReturnsZeroIfEntityIsNull() {
        when(mGeneralInfoDao.getByName("splitsUpdateTimestamp")).thenReturn(null);
        long timestamp = mGeneralInfoStorage.getSplitsUpdateTimestamp();

        assertEquals(0L, timestamp);
    }

    @Test
    public void getSplitsFilterQueryStringReturnsEmptyStringIfEntityIsNull() {
        when(mGeneralInfoDao.getByName("splitsFilterQueryString")).thenReturn(null);
        String queryString = mGeneralInfoStorage.getSplitsFilterQueryString();

        assertEquals("", queryString);
    }

    @Test
    public void getDatabaseEncryptionModeReturnsEmptyStringIfEntityIsNull() {
        when(mGeneralInfoDao.getByName("databaseEncryptionMode")).thenReturn(null);
        String value = mGeneralInfoStorage.getDatabaseEncryptionMode();

        assertEquals("", value);
    }

    @Test
    public void getFlagsSpecReturnsEmptyStringIfEntityIsNull() {
        when(mGeneralInfoDao.getByName("flagsSpec")).thenReturn(null);
        String value = mGeneralInfoStorage.getFlagsSpec();

        assertEquals("", value);
    }

    @Test
    public void getRolloutCacheLastClearTimestampReturnsZeroIfEntityIsNull() {
        when(mGeneralInfoDao.getByName("rolloutCacheLastClearTimestamp")).thenReturn(null);
        long timestamp = mGeneralInfoStorage.getRolloutCacheLastClearTimestamp();

        assertEquals(0L, timestamp);
    }

    @Test
    public void getRbsChangeNumberReturnsMinusOneIfEntityIsNull() {
        when(mGeneralInfoDao.getByName("rbsChangeNumber")).thenReturn(null);
        long changeNumber = mGeneralInfoStorage.getRbsChangeNumber();

        assertEquals(-1L, changeNumber);
    }

    @Test
    public void getRbsChangeNumberReturnsValueFromDao() {
        when(mGeneralInfoDao.getByName("rbsChangeNumber")).thenReturn(new GeneralInfoEntity("rbsChangeNumber", 123L));
        long changeNumber = mGeneralInfoStorage.getRbsChangeNumber();

        assertEquals(123L, changeNumber);
        verify(mGeneralInfoDao).getByName("rbsChangeNumber");
    }

    @Test
    public void setRbsChangeNumberSetsValueOnDao() {
        mGeneralInfoStorage.setRbsChangeNumber(123L);

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals("rbsChangeNumber") && entity.getLongValue() == 123L));
    }

    @Test
    public void getProxyConfigReturnsValueFromDao() {
        when(mGeneralInfoDao.getByName("proxyConfig"))
                .thenReturn(new GeneralInfoEntity("proxyConfig", "encrypted_proxyConfigValue"));
        String proxyConfig = mGeneralInfoStorage.getProxyConfig();

        assertEquals("decrypted_encrypted_proxyConfigValue", proxyConfig);
        verify(mGeneralInfoDao).getByName("proxyConfig");
        verify(mAlwaysEncryptedSplitCipher).decrypt("encrypted_proxyConfigValue");
    }

    @Test
    public void getProxyConfigReturnsNullIfEntityIsNull() {
        when(mGeneralInfoDao.getByName("proxyConfig")).thenReturn(null);
        String proxyConfig = mGeneralInfoStorage.getProxyConfig();

        assertNull(proxyConfig);
    }

    @Test
    public void setProxyConfigSetsValueOnDao() {
        mGeneralInfoStorage.setProxyConfig("proxyConfigValue");

        verify(mAlwaysEncryptedSplitCipher).encrypt("proxyConfigValue");
        verify(mGeneralInfoDao).update(argThat(entity ->
                entity.getName().equals("proxyConfig") &&
                entity.getStringValue().equals("encrypted_proxyConfigValue")));
    }
    
    @Test
    public void testSerializeAndStoreHttpProxy() {
        String testHost = "proxy.example.com";
        int testPort = 8080;
        String testUsername = "testuser";
        String testPassword = "testpass";
        String testClientCert = "-----BEGIN CERTIFICATE-----\nMIICertificateContent\n-----END CERTIFICATE-----";
        String testClientKey = "-----BEGIN PRIVATE KEY-----\nMIIKeyContent\n-----END PRIVATE KEY-----";
        String testCaCert = "-----BEGIN CA CERTIFICATE-----\nMIICACertContent\n-----END CA CERTIFICATE-----";
        
        InputStream clientCertStream = new ByteArrayInputStream(testClientCert.getBytes(StandardCharsets.UTF_8));
        InputStream clientKeyStream = new ByteArrayInputStream(testClientKey.getBytes(StandardCharsets.UTF_8));
        InputStream caCertStream = new ByteArrayInputStream(testCaCert.getBytes(StandardCharsets.UTF_8));
        
        ProxyCredentialsProvider credentialsProvider = mock(ProxyCredentialsProvider.class);

        HttpProxy httpProxy = HttpProxy.newBuilder(testHost, testPort)
                .basicAuth(testUsername, testPassword)
                .mtlsAuth(clientCertStream, clientKeyStream)
                .proxyCacert(caCertStream)
                .credentialsProvider(credentialsProvider)
                .build();
        
        HttpProxySerializer.serializeAndStore(httpProxy, mGeneralInfoStorage);
        
        verify(mGeneralInfoDao).update(argThat(entity ->
            entity.getName().equals("proxyConfig") && 
            entity.getStringValue().startsWith("encrypted_")));
    }
    
    @Test
    public void testGetProxyConfig() {
        String jsonContent = "{\"host\":\"proxy.example.com\",\"port\":8080,\"username\":\"testuser\",\"password\":\"testpass\",\"client_cert\":\"cert-data\",\"client_key\":\"key-data\",\"ca_cert\":\"ca-data\",\"bearer_token\":\"token\"}";
        when(mAlwaysEncryptedSplitCipher.encrypt(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return invocation.getArgument(0);
            }
        });
        when(mAlwaysEncryptedSplitCipher.decrypt(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return invocation.getArgument(0);
            }
        });
        when(mGeneralInfoDao.getByName("proxyConfig")).thenReturn(new GeneralInfoEntity("proxyConfig", jsonContent));
        
        String proxyConfigJson = mGeneralInfoStorage.getProxyConfig();
        
        assertNotNull("Proxy config JSON should not be null", proxyConfigJson);
        
        HttpProxyDto dto = HttpProxySerializer.deserialize(mGeneralInfoStorage);
        assertNotNull("Deserialized DTO should not be null", dto);
        assertEquals("Host should match", "proxy.example.com", dto.host);
        assertEquals("Port should match", 8080, dto.port);
        assertEquals("Username should match", "testuser", dto.username);
        assertEquals("Password should match", "testpass", dto.password);
        assertEquals("Client cert should match", "cert-data", dto.clientCert);
        assertEquals("Client key should match", "key-data", dto.clientKey);
        assertEquals("CA cert should match", "ca-data", dto.caCert);
        assertEquals("token", dto.bearerToken);
    }
    
    @Test
    public void proxyConfigIsNullWhenStoredDataIsNull() {
        when(mGeneralInfoDao.getByName("proxyConfig")).thenReturn(null);
        
        String proxyConfig = mGeneralInfoStorage.getProxyConfig();
        
        assertNull("Proxy config should be null when entity is null", proxyConfig);
    }
    
    @Test
    public void proxyConfigIsNullWhenTheStoredValueIsNull() {
        GeneralInfoEntity entity = new GeneralInfoEntity("proxyConfig", (String) null);
        when(mGeneralInfoDao.getByName("proxyConfig")).thenReturn(entity);
        
        String proxyConfig = mGeneralInfoStorage.getProxyConfig();
        
        assertNull("Proxy config should be null when entity value is null", proxyConfig);
    }
    
    @Test
    public void proxyConfigCanBeSetToNull() {
        mGeneralInfoStorage.setProxyConfig(null);
        
        verify(mGeneralInfoDao).update(argThat(entity ->
            entity.getName().equals("proxyConfig") && 
            entity.getStringValue() == null));
    }
}
