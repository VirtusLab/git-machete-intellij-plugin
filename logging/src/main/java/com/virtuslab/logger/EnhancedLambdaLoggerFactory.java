package com.virtuslab.logger;

public final class EnhancedLambdaLoggerFactory {
  private EnhancedLambdaLoggerFactory() {}

  public static IEnhancedLambdaLogger create(Class<?> clazz) {
    return new EnhancedLambdaLogger(clazz);
  }
}
