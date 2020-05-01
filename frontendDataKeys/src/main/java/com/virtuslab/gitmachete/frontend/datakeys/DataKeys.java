package com.virtuslab.gitmachete.frontend.datakeys;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.nio.file.Path;

import com.intellij.openapi.actionSystem.DataKey;
import git4idea.repo.GitRepository;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.ui.api.table.IGraphTableManager;

public final class DataKeys {
  private DataKeys() {}

  public static final DataKey<@Nullable IBranchLayout> KEY_BRANCH_LAYOUT = DataKey.create("BRANCH_LAYOUT");
  public static final DataKey<@Nullable Path> KEY_GIT_MACHETE_FILE_PATH = DataKey.create("GIT_MACHETE_FILE_PATH");
  public static final DataKey<@Nullable IGitMacheteRepository> KEY_GIT_MACHETE_REPOSITORY = DataKey
      .create("GIT_MACHETE_REPOSITORY");
  /** This key must always be available in the container hierarchy, and a DataProvider must always return a non-null value. */
  public static final DataKey<@NonNull IGraphTableManager> KEY_GRAPH_TABLE_MANAGER = DataKey.create("GRAPH_TABLE_MANAGER");
  public static final DataKey<@Nullable String> KEY_SELECTED_BRANCH_NAME = DataKey.create("SELECTED_BRANCH_NAME");
  public static final DataKey<@Nullable GitRepository> KEY_SELECTED_VCS_REPOSITORY = DataKey.create("SELECTED_VCS_REPOSITORY");

  public static <T> Match.Case<String, T> typeSafeCase(DataKey<T> key, T value) {
    return Case($(key.getName()), value);
  }
}
