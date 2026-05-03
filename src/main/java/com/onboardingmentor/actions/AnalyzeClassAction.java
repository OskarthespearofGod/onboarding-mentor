package com.onboardingmentor.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.onboardingmentor.ai.MentorAgentClient;
import com.onboardingmentor.context.ClassContext;
import com.onboardingmentor.context.ClassContextExtractor;
import com.onboardingmentor.settings.MentorSettings;
import com.onboardingmentor.ui.MentorToolWindow;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.ActionUpdateThread;

public class AnalyzeClassAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null || editor == null || psiFile == null) return;

        String apiKey = MentorSettings.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Notification notification = new Notification(
                    "Onboarding Mentor",
                    "Missing API Key",
                    "Set your Anthropic API key in Settings > Tools > Onboarding Mentor",
                    NotificationType.WARNING
            );
            Notifications.Bus.notify(notification, project);
            return;
        }

        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);

        if (psiClass == null) {
            Notification notification = new Notification(
                    "Onboarding Mentor",
                    "No Class Found",
                    "Please right-click inside a Java class.",
                    NotificationType.INFORMATION
            );
            Notifications.Bus.notify(notification, project);
            return;
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Onboarding Mentor");
        if (toolWindow != null) {
            toolWindow.show();
        }

        MentorToolWindow mentorWindow = MentorToolWindow.getInstance(project);
        if (mentorWindow != null) {
            mentorWindow.displayAnalyzing();
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Onboarding Mentor: Analyzing Class", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    ClassContext context = ReadAction.compute(() -> ClassContextExtractor.extract(psiClass));

                    MentorAgentClient client = new MentorAgentClient(project);
                    String result = client.analyzeClass(context);

                    if (mentorWindow != null) {
                        mentorWindow.display(result);
                    }
                } catch (Exception ex) {
                    if (mentorWindow != null) {
                        mentorWindow.displayError(ex.getMessage() != null ? ex.getMessage() : ex.toString());
                    }
                }
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabledAndVisible(project != null && editor != null && psiFile != null);
    }
}
