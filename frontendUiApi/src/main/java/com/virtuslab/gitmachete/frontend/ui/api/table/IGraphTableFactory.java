package com.virtuslab.gitmachete.frontend.ui.api.table;

import com.intellij.openapi.project.Project;

public interface IGraphTableFactory {
  BaseGraphTable create(Project project);
}
