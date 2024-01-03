package io.split.android.client.network;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

public class SplitBasicAuthenticatorTest {

    private SplitBasicAuthenticator.Base64Encoder mBase64Encoder;

    @Before
    public void setUp() {
        mBase64Encoder = mock(SplitBasicAuthenticator.Base64Encoder.class);
        when(mBase64Encoder.encode(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void callingAuthenticateUsesEncoder() {
        SplitBasicAuthenticator authenticator = new SplitBasicAuthenticator("user", "pass", mBase64Encoder);
        authenticator.authenticate(mock(SplitAuthenticatedRequest.class));

        verify(mBase64Encoder).encode("user:pass");
    }

    @Test
    public void callingAuthenticateReturnsCorrectHeaderInRequest() {
        SplitBasicAuthenticator authenticator = new SplitBasicAuthenticator("user", "pass", mBase64Encoder);
        SplitAuthenticatedRequest request = authenticator.authenticate(mock(SplitAuthenticatedRequest.class));

        verify(request).setHeader("Proxy-Authorization", "Basic user:pass");
    }
}
