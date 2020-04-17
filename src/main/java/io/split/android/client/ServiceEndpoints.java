package io.split.android.client;

import androidx.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

public class ServiceEndpoints {

    private static final String API_ENDPOINT = "https://sdk.split.io/api";
    private static final String EVENTS_ENDPOINT = "https://events.split.io/api";
    private static final String SSE_AUTH_SERVICE_ENDPOINT = "https://auth.split-stage.io/api";
    private static final String STREAMING_SERVICE_ENDPOINT = "https://realtime.ably.io/sse";

    private String mApiEndpoint = API_ENDPOINT;
    private String mEventsEndpoint = EVENTS_ENDPOINT;
    private String mSseAuthServiceEndpoint = SSE_AUTH_SERVICE_ENDPOINT;
    private String mStreamingServiceEndpoint = STREAMING_SERVICE_ENDPOINT;

    private ServiceEndpoints() {
    }

    public String getApiEndpoint() {
        return mApiEndpoint;
    }

    private void setApiEndpoint(String endpoint) {
        this.mApiEndpoint = endpoint;
    }

    public String getEventsEndpoint() {
        return mEventsEndpoint;
    }

    private void setEventsEndpoint(String endpoint) {
        this.mEventsEndpoint = endpoint;
    }

    public String getAuthServiceEndpoint() {
        return mSseAuthServiceEndpoint;
    }

    private void setAuthServiceEndpoint(String endpoint) {
        this.mSseAuthServiceEndpoint = endpoint;
    }

    public String getStreamingServiceEndpoint() {
        return mStreamingServiceEndpoint;
    }

    private void setStreamingServiceEndpoint(String endpoint) {
        this.mStreamingServiceEndpoint = endpoint;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        ServiceEndpoints mServiceEndpoints;

        protected Builder() {
            mServiceEndpoints = new ServiceEndpoints();
        }

        /**
         * The rest endpoint that sdk will hit for latest features and segments.
         *
         * @param apiEndpoint MUST NOT be null
         * @return this builder
         */
        public Builder apiEndpoint(@NonNull String url) {
            mServiceEndpoints.setApiEndpoint(checkNotNull(url));
            return this;
        }

        /**
         * The rest endpoint that sdk will hit to send events and impressions
         *
         * @param eventsEndpoint MUST NOT be null
         * @return this builder
         */
        public Builder eventsEndpoint(@NonNull String url) {
            mServiceEndpoints.setEventsEndpoint(checkNotNull(url));
            return this;
        }

        /**
         * The rest endpoint that sdk will hit to get an SSE authentication token
         * to subscribe to SSE channels and receive update events
         *
         * @param authServiceEndpoint MUST NOT be null
         * @return this builder
         */
        public Builder sseAuthServiceEndpoint(@NonNull String url) {
            mServiceEndpoints.setAuthServiceEndpoint(checkNotNull(url));
            return this;
        }

        /**
         * The rest endpoint that sdk will hit to subscribe to SSE channels
         * and receive update events
         *
         * @param streamingServiceEndpoint MUST NOT be null
         * @return this builder
         */
        public Builder streamingServiceEndpoint(@NonNull String url) {
            mServiceEndpoints.setStreamingServiceEndpoint(checkNotNull(url));
            return this;
        }

        public ServiceEndpoints build() {
            if ((API_ENDPOINT.equals(mServiceEndpoints.getApiEndpoint()) &&
                    !EVENTS_ENDPOINT.equals(mServiceEndpoints.getEventsEndpoint())) ||
                    (!API_ENDPOINT.equals(mServiceEndpoints.getApiEndpoint()) &&
                            EVENTS_ENDPOINT.equals(mServiceEndpoints.getEventsEndpoint()))
            ) {
                throw new IllegalArgumentException("If endpoint is set, you must also set the events endpoint");
            }
            return mServiceEndpoints;
        }
    }
}