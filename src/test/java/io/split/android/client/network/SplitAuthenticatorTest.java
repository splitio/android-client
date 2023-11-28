package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
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
        Map<String, List<String>> initialHeaders = new HashMap<>(request.getRequest().getHeaders());

        splitAuthenticator.authenticate(request);

        Map<String, List<String>> finalHeaders = new HashMap<>(request.getRequest().getHeaders());

        assertEquals(2, initialHeaders.size());
        assertTrue(initialHeaders.containsKey("header1"));
        assertTrue(initialHeaders.containsKey("header2"));
        assertEquals(Collections.singletonList("value1"), initialHeaders.get("header1"));
        assertEquals(Collections.singletonList("value2"), initialHeaders.get("header2"));
        assertEquals(3, finalHeaders.size());
        assertTrue(finalHeaders.containsKey("header1"));
        assertTrue(finalHeaders.containsKey("header2"));
        assertTrue(finalHeaders.containsKey("new-header"));
        assertEquals(Collections.singletonList("value1"), finalHeaders.get("header1"));
        assertEquals(Collections.singletonList("value2"), finalHeaders.get("header2"));
        assertEquals(Collections.singletonList("value"), finalHeaders.get("new-header"));
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
        public Map<String, List<String>> getHeaders() {
            return mRequest.getHeaders();
        }

        @Override
        public MockRequest getRequest() {
            return mRequest;
        }

        @Override
        public int getStatusCode() {
            return 0;
        }

        @Override
        public String getRequestUrl() {
            return null;
        }
    }

    private static class MockRequest {
        private final Map<String, List<String>> mHeaders = new ConcurrentHashMap<>();

        MockRequest() {
            mHeaders.put("header1", Collections.singletonList("value1"));
            mHeaders.put("header2", Collections.singletonList("value2"));
        }

        public void setHeader(@NonNull String name, @NonNull String value) {
            if (mHeaders.get(name) != null) {
                mHeaders.get(name).add(value);
            } else {
                mHeaders.put(name, Collections.singletonList(value));
            }
        }

        public String getHeader(@NonNull String name) {
            if (mHeaders.get(name) != null && mHeaders.get(name).size() > 0) {
                return mHeaders.get(name).get(0);
            }

            return null;
        }

        public Map<String, List<String>> getHeaders() {
            return mHeaders;
        }
    }
}
