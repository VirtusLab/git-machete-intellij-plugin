package com.virtuslab.gitmachete.frontend.datakeys;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.intellij.openapi.actionSystem.DataKey;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;

public final class DataKeys {
  private DataKeys() {}

  public static final DataKey<@Nullable IGitMacheteRepository> KEY_GIT_MACHETE_REPOSITORY = DataKey
      .create("GIT_MACHETE_REPOSITORY");
  public static final DataKey<@Nullable String> KEY_SELECTED_BRANCH_NAME = DataKey.create("SELECTED_BRANCH_NAME");

  // Note: this method isn't currently fully null-safe, it's possible to pass {@code null} as {@code value}
  // even if {@code T} is marked as {@code @NonNull}.
  // See https://github.com/typetools/checker-framework/issues/3289
  // and generally https://github.com/typetools/checker-framework/issues/979.
  public static <T> Match.Case<String, T> typeSafeCase(DataKey<T> key, T value) {
    return Case($(key.getName()), value);
  }
}
