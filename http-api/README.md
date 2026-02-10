# HTTP API module

Public contracts and configuration types for the HTTP client.
These types are exposed to SDK consumers through the `:main` module's `api` dependency.

## `HttpClientConfiguration`

Bundles all HTTP client settings into a single object:

```java
HttpClientConfiguration config = HttpClientConfiguration.builder()
        .connectionTimeout(15_000)
        .readTimeout(15_000)
        .proxy(proxy)
        .proxyAuthenticator(authenticator)
        .certificatePinningConfiguration(pinConfig)
        .developmentSslConfig(devSsl)
        .build();
```

## Proxy configuration

### Basic auth

```java
HttpProxy proxy = HttpProxy.newBuilder("proxy.example.com", 8080)
        .basicAuth("user", "pass")
        .build();
```

### mTLS with custom CA

```java
HttpProxy proxy = HttpProxy.newBuilder("proxy.example.com", 8443)
        .proxyCacert(caCertInputStream)
        .mtls(clientCertInputStream, clientKeyInputStream)
        .build();
```

### Custom credentials provider

```java
// Bearer token
HttpProxy proxy = HttpProxy.newBuilder("proxy.example.com", 8080)
        .credentialsProvider(() -> fetchBearerToken())
        .build();

// Basic credentials
HttpProxy proxy = HttpProxy.newBuilder("proxy.example.com", 8080)
        .credentialsProvider(new BasicCredentialsProvider() {
            public String getUsername() { return "user"; }
            public String getPassword() { return "pass"; }
        })
        .build();
```

## Custom proxy authenticator

Implement `SplitAuthenticator` to handle proxy challenge/response flows:

```java
SplitAuthenticator authenticator = new SplitAuthenticator() {
    @Override
    public AuthenticatedRequest authenticate(@NonNull AuthenticatedRequest request) {
        request.setHeader("Proxy-Authorization", "Bearer " + getToken());
        return request;
    }
};
```

The `AuthenticatedRequest` gives access to existing headers and the request URL, so the authenticator can make decisions based on context.

## Certificate pinning

```java
CertificatePinningConfiguration pinConfig = CertificatePinningConfiguration.builder()
        // Pin by hash (sha256 or sha1)
        .addPin("sdk.split.io", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        // Pin from a certificate file (derives hashes automatically)
        .addPin("*.split.io", certInputStream)
        // Optional: get notified on pin failures
        .failureListener((host, certificateChain) -> {
            Log.w("Split", "Pin failed for " + host
                    + ", chain size: " + certificateChain.size());
        })
        .build();
```

Wildcard hosts are supported: `*.example.com` matches one subdomain, `**.example.com` matches any depth.

## Development SSL overrides

For test environments with self-signed certificates:

```java
DevelopmentSslConfig devSsl = new DevelopmentSslConfig(trustManager, hostnameVerifier);

// Or, if you already have an SSLSocketFactory:
DevelopmentSslConfig devSsl = new DevelopmentSslConfig(sslSocketFactory, trustManager, hostnameVerifier);
```
