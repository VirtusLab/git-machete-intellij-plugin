package com.virtuslab.logger;

import kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory;

public final class MacheteLoggerFactory {
  private MacheteLoggerFactory() {}

  public static IMacheteLogger getLogger(String name) {
    return new MacheteLogger(LambdaLoggerFactory.getLogger(name));
  }

  public static IMacheteLogger getLogger(Class<?> clazz) {
    return new MacheteLogger(LambdaLoggerFactory.getLogger(clazz));
  }
}
