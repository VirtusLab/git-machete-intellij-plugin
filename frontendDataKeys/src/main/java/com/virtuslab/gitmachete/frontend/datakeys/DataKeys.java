package com.virtuslab.gitmachete.frontend.datakeys;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.nio.file.Path;

import com.intellij.openapi.actionSystem.DataKey;
import git4idea.repo.GitRepository;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.ui.api.table.IGraphTableManager;

public final class DataKeys {
  private DataKeys() {}

  public static final DataKey<IBranchLayout> KEY_BRANCH_LAYOUT = DataKey.create("BRANCH_LAYOUT");
  public static final DataKey<Path> KEY_GIT_MACHETE_FILE_PATH = DataKey.create("GIT_MACHETE_FILE_PATH");
  public static final DataKey<IGraphTableManager> KEY_GRAPH_TABLE_MANAGER = DataKey.create("GRAPH_TABLE_MANAGER");
  public static final DataKey<IGitMacheteRepository> KEY_GIT_MACHETE_REPOSITORY = DataKey.create("GIT_MACHETE_REPOSITORY");
  public static final DataKey<Boolean> KEY_IS_GIT_MACHETE_REPOSITORY_READY = DataKey.create("IS_GIT_MACHETE_REPOSITORY_READY");
  public static final DataKey<String> KEY_SELECTED_BRANCH_NAME = DataKey.create("SELECTED_BRANCH_NAME");
  public static final DataKey<GitRepository> KEY_SELECTED_VCS_REPOSITORY = DataKey.create("SELECTED_VCS_REPOSITORY");

  public static <T> Match.Case<String, T> typeSafeCase(DataKey<T> key, T value) {
    return Case($(key.getName()), value);
  }

}
