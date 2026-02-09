# HTTP module

Internal HTTP client for the Split SDK. Not exposed to SDK consumers.

## Building an `HttpClient`

Use `HttpClientImpl.Builder` to create an instance:

```java
HttpClient client = new HttpClientImpl.Builder()
        .setConnectionTimeout(15_000)
        .setReadTimeout(15_000)
        .setTlsUpdater(tlsUpdater)                       // optional – TlsUpdater SPI
        .setProxy(httpProxy)                              // optional – proxy config from :http-domain
        .setProxyAuthenticator(authenticator)              // optional – SplitAuthenticator from :http-domain
        .setCertificatePinningConfiguration(pinConfig)     // optional – cert pins from :http-domain
        .setDevelopmentSslConfig(devSslConfig)             // optional – dev/test SSL overrides
        .build();
```

## Making requests

```java
// Simple GET
HttpRequest req = client.request(uri, HttpMethod.GET);
HttpResponse resp = req.execute();

// POST with body and extra headers
HttpRequest post = client.request(uri, HttpMethod.POST, jsonBody, extraHeaders);
HttpResponse resp = post.execute();

// SSE streaming
HttpStreamRequest stream = client.streamRequest(uri);
HttpStreamResponse streamResp = stream.execute();
```

## Global headers

```java
client.setHeader("Authorization", "Bearer " + apiKey);
client.addHeaders(commonHeaders);

// Streaming-specific headers (only applied to streamRequest calls)
client.setStreamingHeader("SplitSDKClientKey", clientKey);
```

## TLS on older devices

Implement the `TlsUpdater` SPI and pass it to the builder. The client calls `couldBeOld()` to decide whether to force TLS 1.2 via `Tls12OnlySocketFactory`.

## URI building

```java
URI uri = new URIBuilder("https://sdk.split.io/api")
        .addPath("splitChanges")
        .addParameter("since", "-1")
        .build();
```
