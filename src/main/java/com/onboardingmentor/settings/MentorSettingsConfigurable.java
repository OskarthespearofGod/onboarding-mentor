package com.onboardingmentor.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class MentorSettingsConfigurable implements Configurable {
    private JPanel myMainPanel;
    private JPasswordField myApiKeyField;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Onboarding Mentor";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        myApiKeyField = new JPasswordField();
        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Anthropic API key:", myApiKeyField)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        return myMainPanel;
    }

    @Override
    public boolean isModified() {
        String currentKey = MentorSettings.getApiKey() != null ? MentorSettings.getApiKey() : "";
        String newKey = new String(myApiKeyField.getPassword());
        return !currentKey.equals(newKey);
    }

    @Override
    public void apply() {
        MentorSettings.setApiKey(new String(myApiKeyField.getPassword()));
    }

    @Override
    public void reset() {
        myApiKeyField.setText(MentorSettings.getApiKey());
    }

    @Override
    public void disposeUIResources() {
        myMainPanel = null;
        myApiKeyField = null;
    }
}
