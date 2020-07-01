package com.virtuslab.gitmachete.backend.api.hook;

import io.vavr.control.Option;

public interface IExecutionResult {
  int getExitCode();
  Option<String> getStdout();
  Option<String> getStderr();
}
