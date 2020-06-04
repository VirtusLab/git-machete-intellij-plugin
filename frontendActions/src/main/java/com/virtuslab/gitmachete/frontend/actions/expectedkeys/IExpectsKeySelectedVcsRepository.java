package com.virtuslab.gitmachete.frontend.actions.expectedkeys;

import com.intellij.openapi.actionSystem.AnActionEvent;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;

import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

public interface IExpectsKeySelectedVcsRepository {
  default Option<GitRepository> getSelectedVcsRepository(AnActionEvent anActionEvent) {
    return Option.of(anActionEvent.getData(DataKeys.KEY_SELECTED_VCS_REPOSITORY));
  }
}
