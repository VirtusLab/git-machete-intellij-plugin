package com.virtuslab.gitmachete.frontend.actions;

import com.intellij.openapi.actionSystem.DataKey;

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;

public final class DataKeys {
  private DataKeys() {}

  public static final String KEY_GIT_MACHETE_REPOSITORY_STRING = "GIT_MACHETE_REPOSITORY";
  public static final String KEY_SELECTED_BRANCH_NAME_STRING = "SELECTED_BRANCH_NAME";
  public static final String KEY_SELECTED_BRANCH_STRING = "SELECTED_BRANCH";

  public static final DataKey<IGitMacheteRepository> KEY_GIT_MACHETE_REPOSITORY = DataKey
      .create(KEY_GIT_MACHETE_REPOSITORY_STRING);
  public static final DataKey<String> KEY_SELECTED_BRANCH_NAME = DataKey.create(KEY_SELECTED_BRANCH_NAME_STRING);
  public static final DataKey<IGitMacheteBranch> KEY_SELECTED_BRANCH = DataKey.create(KEY_SELECTED_BRANCH_STRING);
}
