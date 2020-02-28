package com.virtuslab.gitmachete.actions;

import com.intellij.openapi.actionSystem.DataKey;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.ui.GitMacheteGraphTableManager;

public final class DataKeyIDs {
  private DataKeyIDs() {}

  public static final String KEY_TABLE_MANAGER_STRING = "TABLE_MANAGER";
  public static final String KEY_SELECTED_BRANCH_NAME_STRING = "SELECTED_BRANCH_NAME";
  public static final String KEY_SELECTED_BRANCH_STRING = "SELECTED_BRANCH";

  public static final DataKey<GitMacheteGraphTableManager> KEY_TABLE_MANAGER =
      DataKey.create(KEY_TABLE_MANAGER_STRING);
  public static final DataKey<String> KEY_SELECTED_BRANCH_NAME =
      DataKey.create(KEY_SELECTED_BRANCH_NAME_STRING);
  public static final DataKey<IGitMacheteBranch> KEY_SELECTED_BRANCH =
      DataKey.create(KEY_SELECTED_BRANCH_STRING);
}
