package com.onboardingmentor.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

public class MentorSettings {
    private static final String SYSTEM_SERVICE_NAME = "Onboarding Mentor Plugin";
    private static final String API_KEY_ACCOUNT = "onboarding-mentor-api-key";

    private static CredentialAttributes getCredentialAttributes() {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName(SYSTEM_SERVICE_NAME, API_KEY_ACCOUNT)
        );
    }

    public static String getApiKey() {
        Credentials credentials = PasswordSafe.getInstance().get(getCredentialAttributes());
        return credentials != null ? credentials.getPasswordAsString() : "";
    }

    public static void setApiKey(String apiKey) {
        Credentials credentials = new Credentials(API_KEY_ACCOUNT, apiKey);
        PasswordSafe.getInstance().set(getCredentialAttributes(), credentials);
    }
}
