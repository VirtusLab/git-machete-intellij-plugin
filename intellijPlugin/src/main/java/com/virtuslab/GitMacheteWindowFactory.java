package com.virtuslab;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

public class GitMacheteWindowFactory implements ToolWindowFactory {
    final GitMacheteUI gitMacheteUI = new GitMacheteUI();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Platform.setImplicitExit(false);
        toolWindow.getComponent().getParent().add(gitMacheteUI);
    }
}
