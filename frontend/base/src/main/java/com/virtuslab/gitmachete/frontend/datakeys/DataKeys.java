package com.virtuslab.gitmachete.frontend.datakeys;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.intellij.openapi.actionSystem.DataKey;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;

public final class DataKeys {
  private DataKeys() {}

  public static final DataKey<@Nullable IGitMacheteRepositorySnapshot> GIT_MACHETE_REPOSITORY_SNAPSHOT = DataKey
      .create("GIT_MACHETE_REPOSITORY_SNAPSHOT");
  public static final DataKey<@Nullable String> SELECTED_BRANCH_NAME = DataKey.create("SELECTED_BRANCH_NAME");
  public static final DataKey<@Nullable String> UNMANAGED_BRANCH_NAME = DataKey.create("UNMANAGED_BRANCH_NAME");

  // Note: this method isn't currently fully null-safe, it's possible to pass {@code null} as {@code value}
  // even if {@code T} is marked as {@code @NonNull}.
  // See https://github.com/typetools/checker-framework/issues/3289
  // and generally https://github.com/typetools/checker-framework/issues/979.
  public static <T> Match.Case<String, T> typeSafeCase(DataKey<T> key, T value) {
    return Case($(key.getName()), value);
  }
}
