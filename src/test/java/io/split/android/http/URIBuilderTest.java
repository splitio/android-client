package io.split.android.http;

import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import io.split.android.client.network.URIBuilder;

public class URIBuilderTest {

    @Test
    public void basicUrl() throws URISyntaxException {

        URI root = new URI("https://api.split.io");
        URI uri = new URIBuilder(root).build();

        Assert.assertEquals("https://api.split.io", uri.toString());
    }

    @Test
    public void basicAndPathUrl() throws URISyntaxException {

        URI root = new URI("https://api.split.io");
        String path = "/internal/api/v2/workspaces";
        URI uri = new URIBuilder(root, path).build();

        Assert.assertEquals("https://api.split.io/internal/api/v2/workspaces", uri.toString());
    }

    @Test
    public void rootWithPathAndPathUrl_NoExcedentSlash() throws URISyntaxException {

        URI root = new URI("https://api.split.io/somepath");
        String path = "internal/api/v2/workspaces";
        URI uri = new URIBuilder(root, path).build();

        Assert.assertEquals("https://api.split.io/somepath/internal/api/v2/workspaces", uri.toString());
    }

    @Test
    public void rootWithPathAndPathUrl_OneExcedentSlash() throws URISyntaxException {

        URI root = new URI("https://api.split.io/somepath");
        String path = "/internal/api/v2/workspaces";
        URI uri = new URIBuilder(root, path).build();

        Assert.assertEquals("https://api.split.io/somepath/internal/api/v2/workspaces", uri.toString());
    }

    @Test
    public void rootWithPathAndPathUrl_TwoExcedentSlash() throws URISyntaxException {

        URI root = new URI("https://api.split.io/somepath/");
        String path = "/internal/api/v2/workspaces";
        URI uri = new URIBuilder(root, path).build();

        Assert.assertEquals("https://api.split.io/somepath/internal/api/v2/workspaces", uri.toString());
    }

    @Test
    public void basicAddParameter() throws URISyntaxException {

        URI root = new URI("https://api.split.io");
        String path = "/internal/api/v2/workspaces";
        URI uri = new URIBuilder(root, path).addParameter("p1", "v1").build();

        Assert.assertEquals("https://api.split.io/internal/api/v2/workspaces?p1=v1", uri.toString());
    }

    @Test
    public void basicAddTwoParameters() throws URISyntaxException {

        URI root = new URI("https://api.split.io");
        String path = "/internal/api/v2/workspaces";
        URI uri = new URIBuilder(root, path)
                .addParameter("p1", "v1")
                .addParameter("p2", "v2")
                .build();

        Assert.assertEquals("https://api.split.io/internal/api/v2/workspaces?p1=v1&p2=v2", uri.toString());
    }

}

