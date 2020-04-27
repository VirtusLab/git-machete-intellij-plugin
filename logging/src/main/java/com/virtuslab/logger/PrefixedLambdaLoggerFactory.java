package com.virtuslab.logger;

import kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory;

public final class PrefixedLambdaLoggerFactory {
  private PrefixedLambdaLoggerFactory() {}

  public static IPrefixedLambdaLogger getLogger(String name) {
    return new PrefixedLambdaLogger(LambdaLoggerFactory.getLogger(name));
  }

  public static IPrefixedLambdaLogger getLogger(Class<?> clazz) {
    return new PrefixedLambdaLogger(LambdaLoggerFactory.getLogger(clazz));
  }
}
