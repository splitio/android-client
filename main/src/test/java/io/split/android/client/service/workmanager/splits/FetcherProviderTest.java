package io.split.android.client.service.workmanager.splits;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.net.URISyntaxException;

import io.split.android.client.network.HttpClient;
import io.split.android.client.service.ServiceFactory;

public class FetcherProviderTest {

    private HttpClient mHttpClient;
    private FetcherProvider mFetcherProvider;

    @Before
    public void setUp() {
        mHttpClient = mock(HttpClient.class);
        mFetcherProvider = new FetcherProvider(mHttpClient, "endpoint");
    }

    @Test
    public void provideFetcherUsesServiceFactory() throws URISyntaxException {
        try (MockedStatic<ServiceFactory> mockedStatic = mockStatic(ServiceFactory.class)) {
            mFetcherProvider.provideFetcher("splitsFilterQueryString");
            mockedStatic.verify(() -> ServiceFactory.getSplitsFetcher(mHttpClient, "endpoint", "splitsFilterQueryString"));
        }
    }
}
