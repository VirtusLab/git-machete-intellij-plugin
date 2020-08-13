package com.virtuslab.gitmachete.backend.api.hooks;

public interface IExecutionResult {
  int getExitCode();
  String getStdout();
  String getStderr();
}
