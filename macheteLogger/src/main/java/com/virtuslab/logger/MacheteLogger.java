package com.virtuslab.logger;

import java.util.function.Supplier;

import kr.pe.kwonnam.slf4jlambda.LambdaLogger;

public class MacheteLogger implements IMacheteLogger {
  private final LambdaLogger LOG;
  private final String className;

  MacheteLogger(LambdaLogger logger) {
    LOG = logger;
    // We are sure that at the moment when this constructor is called we have at least 4 elements in stacktrace array
    @SuppressWarnings("upperbound")
    StackTraceElement element = Thread.currentThread().getStackTrace()[3];
    String[] classPath = element.getClassName().split("\\.");
    className = classPath[classPath.length - 1];
  }

  private String getMethodReferenceName() {
    // We are sure that at the moment when this method is called we have at least 7 elements in stacktrace array
    @SuppressWarnings("upperbound")
    StackTraceElement element = Thread.currentThread().getStackTrace()[6];
    return className + "#" + element.getMethodName();
  }

  public void debug(String format) {
    LOG.debug(() -> getMethodReferenceName() + ": " + format);
  }

  public void debug(Supplier<String> msgSupplier) {
    LOG.debug(() -> getMethodReferenceName() + ": " + msgSupplier.get());
  }
}
