package com.virtuslab.gitmachete.backend.impl.hooks;

import lombok.Data;

import com.virtuslab.gitmachete.backend.api.hooks.IExecutionResult;

@Data(staticConstructor = "of")
public class ExecutionResult implements IExecutionResult {
  private final int exitCode;
  private final String stdout;
  private final String stderr;
}
