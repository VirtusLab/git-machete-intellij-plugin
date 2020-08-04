package com.virtuslab.gitmachete.frontend.ui.api.table;

import com.intellij.openapi.project.Project;

public interface IBaseGraphTableFactory {
  BaseGraphTable create(Project project);
}
