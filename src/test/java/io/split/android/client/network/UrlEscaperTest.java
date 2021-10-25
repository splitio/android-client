package io.split.android.client.network;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class UrlEscaperTest {

    private static final String ORIGIN_URI_STRING = "https://api.split.io:8001/path/Segment/test%2Fuser?queryParam1=value1&queryParam2=value2#fragment";
    private final UrlEscaperImpl urlEscaper = new UrlEscaperImpl();
    private URI originUri;
    private URI httpUrlUri;

    @Before
    public void setUp() throws URISyntaxException {
        originUri = new URI(ORIGIN_URI_STRING);
        httpUrlUri = urlEscaper.getUrl(originUri).toURI();
    }

    @Test
    public void buildUrlMapsPortCorrectly() {
        Assert.assertEquals(originUri.getPort(), httpUrlUri.getPort());
    }

    @Test
    public void buildUrlMapsPathCorrectly() {
        Assert.assertEquals(originUri.getPath(), httpUrlUri.getPath());
    }

    @Test
    public void buildUrlMapsSchemeCorrectly() {
        Assert.assertEquals(originUri.getScheme(), httpUrlUri.getScheme());
    }

    @Test
    public void buildUrlMapsHostCorrectly() {
        Assert.assertEquals(originUri.getHost(), httpUrlUri.getHost());
    }

    @Test
    public void buildUrlMapsFragmentCorrectly() {
        Assert.assertEquals(originUri.getFragment(), httpUrlUri.getFragment());
    }

    @Test
    public void buildUrlMapsQueryCorrectly() {
        Assert.assertEquals(originUri.getQuery(), httpUrlUri.getQuery());
    }

    @Test
    public void buildUrlRetainsOpaque() {
        Assert.assertEquals(originUri.isOpaque(), httpUrlUri.isOpaque());
    }

    @Test
    public void buildUrlAbsoluteOpaque() {
        Assert.assertEquals(originUri.isAbsolute(), httpUrlUri.isAbsolute());
    }
}
