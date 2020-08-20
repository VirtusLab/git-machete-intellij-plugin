package com.virtuslab.gitmachete.frontend.actions.base;

import com.virtuslab.logger.IEnhancedLambdaLogger;

public interface IWithLogger {
  IEnhancedLambdaLogger log();

  /**
   * @return {@code false} if the execution on the current thread is in a context where logging should be avoided
   *         (because e.g. it would lead to a massive spam in the logs);
   *         {@code true} otherwise
   */
  boolean isLoggingAcceptable();
}
