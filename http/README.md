# HTTP module

HTTP client for the Split SDK.

## Building an `HttpClient`

### Minimal

```java
HttpClient client = new HttpClientImpl.Builder()
        .setConnectionTimeout(15_000)
        .setReadTimeout(15_000)
        .build();
```

### With `HttpClientConfiguration` (preferred)

Bundle all settings into a single config object from `:http-api`:

```java
HttpClientConfiguration config = HttpClientConfiguration.builder()
        .connectionTimeout(15_000)
        .readTimeout(15_000)
        .proxy(proxy)                                       // optional
        .proxyAuthenticator(authenticator)                   // optional
        .certificatePinningConfiguration(pinConfig)          // optional
        .developmentSslConfig(devSsl)                        // optional
        .build();

HttpClient client = new HttpClientImpl.Builder()
        .setConfiguration(config)
        .setTlsUpdater(tlsUpdater)  // optional – TlsUpdater
        .build();
```

Individual setter calls on the builder take precedence over the configuration object.

### Proxy

```java
// Basic auth proxy
HttpProxy proxy = HttpProxy.newBuilder("proxy.example.com", 8080)
        .basicAuth("user", "pass")
        .build();

// mTLS proxy with custom CA
HttpProxy mtlsProxy = HttpProxy.newBuilder("proxy.example.com", 8443)
        .proxyCacert(caCertInputStream)
        .mtls(clientCertInputStream, clientKeyInputStream)
        .build();

// With a credentials provider (e.g. bearer token)
HttpProxy bearerProxy = HttpProxy.newBuilder("proxy.example.com", 8080)
        .credentialsProvider(new BearerCredentialsProvider(tokenSupplier))
        .build();
```

### Certificate pinning

```java
CertificatePinningConfiguration pinConfig = CertificatePinningConfiguration.builder()
        .addPin("sdk.split.io", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        .addPin("*.split.io", certInputStream)   // derive pins from a certificate file
        .failureListener(failedHost -> {
            Log.w("Split", "Certificate pinning failed for " + failedHost);
        })
        .build();
```

### Development SSL overrides

For test environments where the server uses a self-signed certificate:

```java
DevelopmentSslConfig devSsl = new DevelopmentSslConfig(trustManager, hostnameVerifier);
```

### TLS on older devices

Implement the `TlsUpdater` SPI and pass it to the builder.
The client calls `couldBeOld()` to decide whether to force TLS 1.2 via `Tls12OnlySocketFactory`.

```java
TlsUpdater tlsUpdater = new LegacyTlsUpdaterAdapter(context); // provided by :main
```

## Making requests

```java
// Simple GET
HttpRequest req = client.request(uri, HttpMethod.GET);
HttpResponse resp = req.execute();

// POST with body
HttpRequest post = client.request(uri, HttpMethod.POST, jsonBody);
HttpResponse resp = post.execute();

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
client.addStreamingHeaders(streamingHeaders);
```

## URI building

```java
URI uri = new URIBuilder(new URI("https://sdk.split.io/api"), "splitChanges")
        .addParameter("since", "-1")
        .build();
```
