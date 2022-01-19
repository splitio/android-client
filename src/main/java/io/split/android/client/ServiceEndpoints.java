package io.split.android.client;

import androidx.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

public class ServiceEndpoints {

    private static final String SDK_ENDPOINT = "https://sdk.split.io/api";
    private static final String EVENTS_ENDPOINT = "https://events.split.io/api";
    private static final String AUTH_SERVICE_ENDPOINT = "https://auth.split.io/api/v2";
    private static final String STREAMING_SERVICE_ENDPOINT = "https://streaming.split.io/sse";
    private static final String TELEMETRY_SERVICE_ENDPOINT = "https://telemetry.split.io/api/v1";

    private String mSdkEndpoint = SDK_ENDPOINT;
    private String mEventsEndpoint = EVENTS_ENDPOINT;
    private String mAuthServiceEndpoint = AUTH_SERVICE_ENDPOINT;
    private String mStreamingServiceEndpoint = STREAMING_SERVICE_ENDPOINT;
    private String mTelemetryServiceEndpoint = TELEMETRY_SERVICE_ENDPOINT;

    private ServiceEndpoints() {
    }

    public String getSdkEndpoint() {
        return mSdkEndpoint;
    }

    private void setSdkEndpoint(String endpoint) {
        this.mSdkEndpoint = endpoint;
    }

    public String getEventsEndpoint() {
        return mEventsEndpoint;
    }

    private void setEventsEndpoint(String endpoint) {
        this.mEventsEndpoint = endpoint;
    }

    public String getAuthServiceEndpoint() {
        return mAuthServiceEndpoint;
    }

    private void setAuthServiceEndpoint(String endpoint) {
        this.mAuthServiceEndpoint = endpoint;
    }

    public String getStreamingServiceEndpoint() {
        return mStreamingServiceEndpoint;
    }

    private void setStreamingServiceEndpoint(String endpoint) {
        this.mStreamingServiceEndpoint = endpoint;
    }

    public void setTelemetryServiceEndpoint(String endpoint) {
        this.mTelemetryServiceEndpoint = endpoint;
    }

    public String getTelemetryEndpoint() {
        return mTelemetryServiceEndpoint;
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
         * @param url MUST NOT be null
         * @return this builder
         */
        public Builder apiEndpoint(@NonNull String url) {
            mServiceEndpoints.setSdkEndpoint(checkNotNull(url));
            return this;
        }

        /**
         * The rest endpoint that sdk will hit to send events and impressions
         *
         * @param url MUST NOT be null
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
         * @param url MUST NOT be null
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
         * @param url MUST NOT be null
         * @return this builder
         */
        public Builder streamingServiceEndpoint(@NonNull String url) {
            mServiceEndpoints.setStreamingServiceEndpoint(checkNotNull(url));
            return this;
        }

        /**
         * The endpoint that sdk will hit to send telemetry.
         *
         * @param url MUST NOT be null
         * @return this builder
         */
        public Builder telemetryServiceEndpoint(@NonNull String url) {
            mServiceEndpoints.setTelemetryServiceEndpoint(checkNotNull(url));
            return this;
        }

        public ServiceEndpoints build() {
            return mServiceEndpoints;
        }
    }

    public static class EndpointValidator {
        public static boolean sdkEndpointIsOverridden(String endpoint) {
            return !SDK_ENDPOINT.equals(endpoint);
        }

        public static boolean eventsEndpointIsOverridden(String endpoint) {
            return !EVENTS_ENDPOINT.equals(endpoint);
        }

        public static boolean streamingEndpointIsOverridden(String endpoint) {
            return !STREAMING_SERVICE_ENDPOINT.equals(endpoint);
        }

        public static boolean authEndpointIsOverridden(String endpoint) {
            return !AUTH_SERVICE_ENDPOINT.equals(endpoint);
        }

        public static boolean telemetryEndpointIsOverridden(String endpoint) {
            return !TELEMETRY_SERVICE_ENDPOINT.equals(endpoint);
        }
    }
}
