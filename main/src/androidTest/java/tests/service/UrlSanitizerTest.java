package tests.service;

import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.split.android.client.network.UrlSanitizerImpl;

public class UrlSanitizerTest {

    private static final String ORIGIN_URI_STRING = "https://api.split.io:8001/path/Segment/test%2Fuser?queryParam1=value1&queryParam2=value2#fragment";
    private final UrlSanitizerImpl urlEscaper = new UrlSanitizerImpl();
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

    @Test
    public void concurrencyTest() throws InterruptedException {
        UrlSanitizerImpl urlSanitizer = new UrlSanitizerImpl();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            threads.add(new Thread(() -> {
                try {
                    URI uri = new URI("http://example.com/path?id=" + Thread.currentThread().getId());
                    URL url = urlSanitizer.getUrl(uri);
                    String s = url.getQuery().split("=")[1];
                    if (!s.equals(String.valueOf(Thread.currentThread().getId()))) {
                        fail("Expected " + Thread.currentThread().getId() + " but got " + s + " instead.");
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }));
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }
}
