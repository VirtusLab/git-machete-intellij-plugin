package com.virtuslab.gitmachete.backend.api.hook;

public interface IExecutionResult {
  int getExitCode();
  String getStdout();
  String getStderr();
}
