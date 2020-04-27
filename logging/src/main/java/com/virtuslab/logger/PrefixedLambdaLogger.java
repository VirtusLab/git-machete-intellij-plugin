package com.virtuslab.logger;

import java.util.function.Supplier;

import kr.pe.kwonnam.slf4jlambda.LambdaLogger;

public class PrefixedLambdaLogger implements IPrefixedLambdaLogger {
  private final LambdaLogger logger;
  private final String className;

  PrefixedLambdaLogger(LambdaLogger logger) {
    this.logger = logger;
    // We are sure that at the moment when this constructor is called we have at least 4 elements in stacktrace array:
    // #0: java.lang.Thread#getStackTrace
    // #1: com.virtuslab.logger.MacheteLogger#<init>
    // #2: com.virtuslab.logger.MacheteLoggerFactory#getLogger
    // #3: method from MacheteLoggerFactory#getLogger was called
    @SuppressWarnings("upperbound")
    StackTraceElement element = Thread.currentThread().getStackTrace()[3];
    String[] classPath = element.getClassName().split("\\.");
    className = classPath[classPath.length - 1];
  }

  private String getLogMessagePrefix() {
    // We are sure that at the moment when this method is called we have at least 7 elements in stacktrace array:
    // #0: java.lang.Thread#getStackTrace
    // #1: com.virtuslab.logger.MacheteLogger#getMethodReferenceName
    // #2: com.virtuslab.logger.MacheteLogger#lambda$debug$0
    // #3: kr.pe.kwonnam.slf4jlambda.defaultlogger.LambdaLoggerLocationAwareImpl#doLog
    // #4: kr.pe.kwonnam.slf4jlambda.LambdaLogger#debug
    // #5: com.virtuslab.logger.MacheteLogger#debug
    // #6: method from IMacheteLogger.<logMethod> was called;
    @SuppressWarnings("upperbound")
    StackTraceElement element = Thread.currentThread().getStackTrace()[6];
    return className + "#" + element.getMethodName() + ": ";
  }

  public void trace(String format) {
    logger.trace(() -> getLogMessagePrefix() + format);
  }

  public void trace(Supplier<String> msgSupplier) {
    logger.trace(() -> getLogMessagePrefix() + msgSupplier.get());
  }

  public void debug(String format) {
    logger.debug(() -> getLogMessagePrefix() + format);
  }

  public void debug(Supplier<String> msgSupplier) {
    logger.debug(() -> getLogMessagePrefix() + msgSupplier.get());
  }

  public void info(String format) {
    logger.info(() -> getLogMessagePrefix() + format);
  }

  public void info(Supplier<String> msgSupplier) {
    logger.info(() -> getLogMessagePrefix() + msgSupplier.get());
  }

  public void warn(String format) {
    logger.warn(() -> getLogMessagePrefix() + format);
  }

  public void warn(Supplier<String> msgSupplier) {
    logger.warn(() -> getLogMessagePrefix() + msgSupplier.get());
  }

  public void error(String format) {
    logger.error(() -> getLogMessagePrefix() + format);
  }

  public void error(Supplier<String> msgSupplier) {
    logger.error(() -> getLogMessagePrefix() + msgSupplier.get());
  }
}
