package com.virtuslab.gitmachete.backend.impl.hooks;

import io.vavr.control.Option;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.hook.IExecutionResult;

@Data
@RequiredArgsConstructor(staticName = "of")
public class ExecutionResult implements IExecutionResult {
  private final int exitCode;
  private final @Nullable String stdout;
  private final @Nullable String stderr;

  @Override
  public Option<String> getStdout() {
    return Option.of(stdout);
  }

  @Override
  public Option<String> getStderr() {
    return Option.of(stderr);
  }
}
