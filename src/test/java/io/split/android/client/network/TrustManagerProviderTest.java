package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.MockedStatic;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class TrustManagerProviderTest {

    @Test
    public void getDefaultTrustManagerGetsTrustManagerFactoryWithDefaultAlgorithmAndInitializesIt() throws NoSuchAlgorithmException, KeyStoreException {
        try (MockedStatic<TrustManagerFactory> ignored = mockStatic(TrustManagerFactory.class)) {
            TrustManagerFactory mockedTrustManagerFactory = mock(TrustManagerFactory.class);
            when(TrustManagerFactory.getDefaultAlgorithm()).thenReturn("DefaultAlgo");
            when(mockedTrustManagerFactory.getTrustManagers()).thenReturn(new TrustManager[]{mock(TrustManager.class)});
            when(TrustManagerFactory.getInstance("DefaultAlgo")).thenReturn(mockedTrustManagerFactory);

            TrustManagerProvider.getDefaultTrustManager();

            verify(mockedTrustManagerFactory).init((KeyStore) null);
        }
    }

    @Test
    public void getDefaultTrustManagerReturnsOnlyOneInstanceOfX509TrustManager() throws NoSuchAlgorithmException {
        try (MockedStatic<TrustManagerFactory> ignored = mockStatic(TrustManagerFactory.class)) {
            TrustManagerFactory mockedTrustManagerFactory = mock(TrustManagerFactory.class);
            when(TrustManagerFactory.getDefaultAlgorithm()).thenReturn("DefaultAlgo");
            X509TrustManager mockX509TrustManager = mock(X509TrustManager.class);
            when(mockedTrustManagerFactory.getTrustManagers()).thenReturn(new TrustManager[]{mock(TrustManager.class), mockX509TrustManager});
            when(TrustManagerFactory.getInstance("DefaultAlgo")).thenReturn(mockedTrustManagerFactory);

            X509TrustManager defaultTrustManager = TrustManagerProvider.getDefaultTrustManager();
            assertEquals(mockX509TrustManager, defaultTrustManager);
        }
    }

    @Test
    public void getDefaultTrustManagerReturnsNullWhenThereAreNoneX509TrustManagers() throws NoSuchAlgorithmException {
        try (MockedStatic<TrustManagerFactory> ignored = mockStatic(TrustManagerFactory.class)) {
            TrustManagerFactory mockedTrustManagerFactory = mock(TrustManagerFactory.class);
            when(TrustManagerFactory.getDefaultAlgorithm()).thenReturn("DefaultAlgo");
            when(mockedTrustManagerFactory.getTrustManagers()).thenReturn(new TrustManager[]{mock(TrustManager.class), mock(TrustManager.class)});
            when(TrustManagerFactory.getInstance("DefaultAlgo")).thenReturn(mockedTrustManagerFactory);

            X509TrustManager defaultTrustManager = TrustManagerProvider.getDefaultTrustManager();
            assertNull(defaultTrustManager);
        }
    }

    @Test
    public void getDefaultTrustManagerReturnsNullWhenNoSuchAlgorithmException() throws NoSuchAlgorithmException {
        try (MockedStatic<TrustManagerFactory> ignored = mockStatic(TrustManagerFactory.class)) {
            when(TrustManagerFactory.getInstance(any())).thenThrow(new NoSuchAlgorithmException());

            X509TrustManager defaultTrustManager = TrustManagerProvider.getDefaultTrustManager();
            assertNull(defaultTrustManager);
        }
    }
}
