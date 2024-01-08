package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SplitAuthenticatorTest {

    @Test
    public void authenticatorModifiesHeaders() {
        Authenticator<AuthenticatedRequest<MockRequest>> splitAuthenticator = new Authenticator<AuthenticatedRequest<MockRequest>>() {
            @Override
            public AuthenticatedRequest<MockRequest> authenticate(@NonNull AuthenticatedRequest<MockRequest> request) {
                request.setHeader("new-header", "value");

                return request;
            }
        };

        AuthenticatedMockRequest request = new AuthenticatedMockRequest(new MockRequest());
        Map<String, String> initialHeaders = new HashMap<>(request.getHeaders());

        splitAuthenticator.authenticate(request);

        Map<String, String> finalHeaders = new HashMap<>(request.getHeaders());

        assertEquals(2, initialHeaders.size());
        assertTrue(initialHeaders.containsKey("header1"));
        assertTrue(initialHeaders.containsKey("header2"));
        assertEquals("value1", initialHeaders.get("header1"));
        assertEquals("value2", initialHeaders.get("header2"));
        assertEquals(3, finalHeaders.size());
        assertTrue(finalHeaders.containsKey("header1"));
        assertTrue(finalHeaders.containsKey("header2"));
        assertTrue(finalHeaders.containsKey("new-header"));
        assertEquals("value1", finalHeaders.get("header1"));
        assertEquals("value2", finalHeaders.get("header2"));
        assertEquals("value", finalHeaders.get("new-header"));
    }

    private static class AuthenticatedMockRequest implements AuthenticatedRequest<MockRequest> {

        private final MockRequest mRequest;

        public AuthenticatedMockRequest(MockRequest request) {
            mRequest = request;
        }

        @Override
        public void setHeader(@NonNull String name, @NonNull String value) {
            mRequest.setHeader(name, value);
        }

        @Override
        public String getHeader(@NonNull String name) {
            return mRequest.getHeader(name);
        }

        @Nullable
        @Override
        public Map<String, String> getHeaders() {
            return mRequest.getHeaders();
        }

        @Override
        public String getRequestUrl() {
            return null;
        }
    }

    private static class MockRequest {
        private final Map<String, String> mHeaders = new ConcurrentHashMap<>();

        MockRequest() {
            mHeaders.put("header1", "value1");
            mHeaders.put("header2", "value2");
        }

        public void setHeader(@NonNull String name, @NonNull String value) {
            mHeaders.put(name, value);
        }

        public String getHeader(@NonNull String name) {
            return mHeaders.get(name);
        }

        public Map<String, String> getHeaders() {
            return mHeaders;
        }
    }
}
