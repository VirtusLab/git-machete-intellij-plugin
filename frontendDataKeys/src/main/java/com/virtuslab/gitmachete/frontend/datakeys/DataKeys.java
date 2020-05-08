package com.virtuslab.gitmachete.frontend.datakeys;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.intellij.openapi.actionSystem.DataKey;
import git4idea.repo.GitRepository;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.manager.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseGraphTable;

public final class DataKeys {
  private DataKeys() {}

  /** This key must always be available in the container hierarchy, and a DataProvider must always return a non-null value. */
  public static final DataKey<@NonNull IBranchLayoutWriter> KEY_BRANCH_LAYOUT_WRITER = DataKey.create("BRANCH_LAYOUT_WRITER");
  public static final DataKey<@Nullable IGitMacheteRepository> KEY_GIT_MACHETE_REPOSITORY = DataKey
      .create("GIT_MACHETE_REPOSITORY");
  /** This key must always be available in the container hierarchy, and a DataProvider must always return a non-null value. */
  public static final DataKey<@NonNull BaseGraphTable> KEY_GRAPH_TABLE = DataKey.create("GRAPH_TABLE");
  public static final DataKey<@Nullable String> KEY_SELECTED_BRANCH_NAME = DataKey.create("SELECTED_BRANCH_NAME");
  public static final DataKey<@Nullable GitRepository> KEY_SELECTED_VCS_REPOSITORY = DataKey.create("SELECTED_VCS_REPOSITORY");

  // Note: this method isn't currently fully null-safe, it's possible to pass {@code null} as {@code value}
  // even if {@code T} is marked as {@code @NonNull}.
  // See https://github.com/typetools/checker-framework/issues/3289
  // and generally https://github.com/typetools/checker-framework/issues/979.
  public static <T> Match.Case<String, T> typeSafeCase(DataKey<T> key, T value) {
    return Case($(key.getName()), value);
  }
}
