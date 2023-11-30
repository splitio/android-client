package io.split.android.client.network;

import static org.mockito.Mockito.mock;

import org.junit.Before;

public class SplitUrlConnectionAuthenticatorTest {

    private SplitUrlConnectionAuthenticator mAuthenticator;
    private SplitAuthenticator mSplitAuthenticator;

    @Before
    public void setUp() {
        mSplitAuthenticator = mock(SplitAuthenticator.class);
        mAuthenticator = new SplitUrlConnectionAuthenticator(mSplitAuthenticator);
    }

    //TODO
}
