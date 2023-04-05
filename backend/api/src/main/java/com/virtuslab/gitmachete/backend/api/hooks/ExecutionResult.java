package com.virtuslab.gitmachete.backend.api.hooks;

import lombok.Data;

@Data
public class ExecutionResult {
  private final int exitCode;
  private final String stdout;
  private final String stderr;
}
