package com.virtuslab.gitmachete.backend.hooks;

import lombok.Data;

@Data
public class ExecutionResult {
  private final int exitCode;
  private final String stdout;
  private final String stderr;
}
