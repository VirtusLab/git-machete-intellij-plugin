package com.virtuslab.gitmachete.frontend.keys;

import com.intellij.openapi.actionSystem.DataKey;
import git4idea.repo.GitRepository;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;

public final class DataKeys {
  private DataKeys() {}

  public static final DataKey<Boolean> KEY_IS_GIT_MACHETE_REPOSITORY_READY = DataKey
      .create("IS_GIT_MACHETE_REPOSITORY_READY");
  public static final DataKey<IGitMacheteRepository> KEY_GIT_MACHETE_REPOSITORY = DataKey
      .create("GIT_MACHETE_REPOSITORY");
  public static final DataKey<String> KEY_SELECTED_BRANCH_NAME = DataKey.create("SELECTED_BRANCH_NAME");
  public static final DataKey<GitRepository> KEY_SELECTED_VCS_REPOSITORY = DataKey.create("SELECTED_VCS_REPOSITORY");
}
