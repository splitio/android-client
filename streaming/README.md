# Streaming Module

Generic Server-Sent Events (SSE) client library. This module is responsible for connecting to an SSE endpoint, managing the connection lifecycle, and delivering raw parsed events. It has **no knowledge of application-level message semantics** (e.g. Split notifications, authentication, or JWT tokens).

## Components

### Public API

| Class / Interface | Description |
|---|---|
| `EventSourceClient` | Interface for a generic SSE client. Defines `connect(URI, EventHandler)` and `disconnect()`. |
| `EventSourceClient.EventHandler` | Callback interface with `onOpen()`, `onMessage(Map)`, and `onError(boolean)`. |
| `EventSourceClientImpl` | Default implementation that reads an SSE stream line-by-line and dispatches parsed events. |
| `EventStreamParser` | Parses raw SSE stream lines into field→value maps. |

### SPI (Service Provider Interfaces)

| Interface | Description |
|---|---|
| `StreamingTransport` | Provides the HTTP streaming connection. The host application implements this to bridge its HTTP stack. |
| `StreamingTransport.StreamingConnection` | Represents an open connection that can be executed and closed. |
| `StreamingTransport.StreamingResponse` | Wraps the HTTP response, exposing success status, HTTP code, and a `BufferedReader` for the stream. |

## Usage

The consumer is responsible for:

1. **Implementing `StreamingTransport`** — wrapping its HTTP client to provide streaming connections.
2. **Building the URL** — including any authentication tokens, query parameters, and channels.
3. **Calling `EventSourceClient.connect(url, handler)`** — which blocks while the stream is open.
4. **Handling events** — via the `EventHandler` callbacks (`onOpen`, `onMessage`, `onError`).

```java
StreamingTransport transport = new MyHttpTransport(httpClient);
EventStreamParser parser = new EventStreamParser();
EventSourceClient client = new EventSourceClientImpl(transport, parser);

URI url = buildStreamingUrl(token);

client.connect(url, new EventSourceClient.EventHandler() {
    @Override
    public void onOpen() {
        // Connection established
    }

    @Override
    public void onMessage(@NonNull Map<String, String> event) {
        // Handle SSE event (data, event type, id fields)
    }

    @Override
    public void onError(boolean retryable) {
        // Handle connection error
    }
});
```

## Dependencies

- `androidx.annotation` — for nullability annotations
- `:logger` — internal logging module
