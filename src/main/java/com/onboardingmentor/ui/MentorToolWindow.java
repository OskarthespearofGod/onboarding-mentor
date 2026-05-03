package com.onboardingmentor.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MentorToolWindow {
    private final JPanel contentPanel;
    private final JEditorPane editorPane;
    private static final Map<Project, MentorToolWindow> instances = new ConcurrentHashMap<>();

    public MentorToolWindow(Project project, ToolWindow toolWindow) {
        contentPanel = new JPanel(new BorderLayout());
        editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);

        JBScrollPane scrollPane = new JBScrollPane(editorPane);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        instances.put(project, this);
    }

    public JPanel getContent() {
        return contentPanel;
    }

    public static MentorToolWindow getInstance(Project project) {
        return instances.get(project);
    }

    public void displayAnalyzing() {
        SwingUtilities.invokeLater(() -> {
            editorPane.setText("<html><body style='font-family: sans-serif; padding: 10px;'><h3>Analyzing...</h3></body></html>");
        });
    }

    public void displayError(String error) {
        SwingUtilities.invokeLater(() -> {
            editorPane.setText("<html><body style='font-family: sans-serif; padding: 10px; color: red;'><h3>Error</h3><p>" + error + "</p></body></html>");
        });
    }

    public void display(String markdown) {
        Parser parser = Parser.builder().build();
        org.commonmark.node.Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String html = renderer.render(document);

        SwingUtilities.invokeLater(() -> {
            editorPane.setText("<html><body style='font-family: sans-serif; padding: 10px;'>" + html + "</body></html>");
            // Scroll to top
            editorPane.setCaretPosition(0);
        });
    }
}
