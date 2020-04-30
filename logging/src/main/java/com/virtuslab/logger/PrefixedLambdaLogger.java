package com.virtuslab.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
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
    // #3: method from which MacheteLoggerFactory#getLogger was called
    @SuppressWarnings("upperbound")
    StackTraceElement element = Thread.currentThread().getStackTrace()[3];
    String[] classPath = element.getClassName().split("\\.");
    className = classPath[classPath.length - 1];
  }

  private String getStackTraceAsString() {
    // We are sure that at the moment when this method is called we have at least 7 elements in stacktrace array:
    // #0: java.lang.Thread#getStackTrace
    // #1: com.virtuslab.logger.MacheteLogger#getMethodReferenceName
    // #2: com.virtuslab.logger.MacheteLogger#lambda$debug$0
    // #3: kr.pe.kwonnam.slf4jlambda.defaultlogger.LambdaLoggerLocationAwareImpl#doLog
    // #4: kr.pe.kwonnam.slf4jlambda.LambdaLogger#debug
    // #5: com.virtuslab.logger.MacheteLogger#debug
    // #6: method from which IMacheteLogger.<logMethod> was called;
    @SuppressWarnings("upperbound")
    StackTraceElement element = Thread.currentThread().getStackTrace()[6];
    return className + "#" + element.getMethodName() + ": ";
  }

  private String throwableToString(Throwable t) {
    StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }

  @Override
  public void trace(String format) {
    logger.trace(() -> getStackTraceAsString() + format);
  }

  @Override
  public void trace(Supplier<String> msgSupplier) {
    logger.trace(() -> getStackTraceAsString() + msgSupplier.get());
  }

  @Override
  public void debug(String format) {
    logger.debug(() -> getStackTraceAsString() + format);
  }

  @Override
  public void debug(Supplier<String> msgSupplier) {
    logger.debug(() -> getStackTraceAsString() + msgSupplier.get());
  }

  @Override
  public void info(String format) {
    logger.info(() -> getStackTraceAsString() + format);
  }

  @Override
  public void info(Supplier<String> msgSupplier) {
    logger.info(() -> getStackTraceAsString() + msgSupplier.get());
  }

  @Override
  public void warn(String format) {
    logger.warn(() -> getStackTraceAsString() + format);
  }

  @Override
  public void error(String format) {
    logger.error(() -> getStackTraceAsString() + format);
  }

  @Override
  public void error(String format, Throwable t) {
    logger.error(() -> getStackTraceAsString() + format + System.lineSeparator() + throwableToString(t));
  }
}
