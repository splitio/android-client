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

    @Test
    public void defaultQueryStringNoParameters() throws URISyntaxException {

        URI root = new URI("https://api.split.io");
        String path = "/internal/api/v2/workspaces";
        URI uri = new URIBuilder(root, path).defaultQueryString("p1=v1,v2,v3&p2=v1,v2").build();

        Assert.assertEquals("https://api.split.io/internal/api/v2/workspaces?p1=v1,v2,v3&p2=v1,v2", uri.toString());
    }

    @Test
    public void defaultQueryStringAndParameters() throws URISyntaxException {

        URI root = new URI("https://api.split.io");
        String path = "/internal/api/v2/workspaces";
        URI uri = new URIBuilder(root, path).addParameter("p3", "v3").defaultQueryString("p1=v1,v2,v3&p2=v1,v2").build();

        Assert.assertEquals("https://api.split.io/internal/api/v2/workspaces?p3=v3&p1=v1,v2,v3&p2=v1,v2", uri.toString());
    }

    @Test
    public void existingQueryStringAndParameters() throws URISyntaxException {

        URI root = new URI("https://api.split.io/internal/api/v2/workspaces?p1=v1,v2,v3&p2=v1,v2");
        URI uri = new URIBuilder(root).addParameter("p3", "v3").build();

        Assert.assertEquals("https://api.split.io/internal/api/v2/workspaces?p3=v3&p1=v1,v2,v3&p2=v1,v2", uri.toString());
    }

    @Test
    public void encodeNoSafeChars() throws URISyntaxException {

        URI root = new URI("https://api.split.io");
        String path = "/internal/api/v2/work|spaces";
        URI uri = new URIBuilder(root, path).addParameter("p3", "v3").defaultQueryString("p1=v1|1,v2,v3&p2=v1,v2").build();

        Assert.assertEquals("https://api.split.io/internal/api/v2/work%7Cspaces?p3=v3&p1=v1%7C1,v2,v3&p2=v1,v2", uri.toString());
    }

    @Test
    public void maintainSpecialCharactersInPath() throws URISyntaxException {

        URI root = new URI("https://api.split.io/api/mySegments");
        URI uri = new URIBuilder(root, "test%2Fuser").build();

        Assert.assertEquals("/api/mySegments/test%2Fuser", uri.getPath());
    }

    @Test
    public void multipleParamsWithSameNameAreKept() throws URISyntaxException {

        URI root = new URI("https://api.split.io/");
        URIBuilder uriBuilder = new URIBuilder(root);
        uriBuilder.addParameter("users", "user1");
        uriBuilder.addParameter("users", "user2");
        URI uri = uriBuilder.build();

        Assert.assertEquals("users=user1&users=user2", uri.getQuery());
    }

    @Test
    public void addParamsPreservesOrder() throws URISyntaxException {
        URI root = URI.create("https://api.split.io/");
        URIBuilder uriBuilder = new URIBuilder(root);
        uriBuilder.addParameter("param1", "user1");
        uriBuilder.addParameter("param2", "user2");
        uriBuilder.addParameter("param3", "user3");
        URI uri = uriBuilder.build();
        Assert.assertEquals("param1=user1&param2=user2&param3=user3", uri.getQuery());
    }
}
