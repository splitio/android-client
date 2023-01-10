package io.split.android.client;

import io.split.android.client.shared.UserConsent;

public interface UserConsentManager {
    UserConsent getStatus();
    void setStatus(UserConsent status);
}
