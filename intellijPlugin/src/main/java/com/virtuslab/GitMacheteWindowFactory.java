package com.virtuslab;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;

public class GitMacheteWindowFactory implements ToolWindowFactory {

    GitMacheteWindow gmWindow;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        GitMacheteComponent gmComponent = project.getComponent(GitMacheteComponent.class);
    }
}
