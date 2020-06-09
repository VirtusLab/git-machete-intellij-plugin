package com.virtuslab.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.function.Supplier;

import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

public class EnhancedLambdaLogger implements IEnhancedLambdaLogger {
  private final LambdaLogger logger;
  private final String simpleClassName;
  private final ThreadLocal<@Nullable String> bufferedTimerMessage;
  private final ThreadLocal<@Nullable Long> timerStartMillis;

  EnhancedLambdaLogger(Class<?> clazz) {
    String category = clazz
        .getPackageName()
        .replace("com.virtuslab.", "")
        .replaceAll("\\.[^.]+$", "")
        .replaceAll("\\.impl$", "");

    this.logger = LambdaLoggerFactory.getLogger(category);
    this.simpleClassName = clazz.getSimpleName();
    this.bufferedTimerMessage = new ThreadLocal<>();
    this.timerStartMillis = new ThreadLocal<>();
  }

  private static boolean isInternalLoggingClass(String fqcn) {
    return fqcn.equals("java.lang.Thread") || fqcn.startsWith("com.virtuslab.logger.")
        || fqcn.startsWith("kr.pe.kwonnam.slf4jlambda.");
  }

  private String getLogMessagePrefix() {
    // At the moment when this method is called we have the following elements at the top of the stack:
    // #0: java.lang.Thread#getStackTrace
    // #1: com.virtuslab.logger.MacheteLogger#getMethodReferenceName
    // #2: com.virtuslab.logger.MacheteLogger#lambda$debug$0
    // #3: kr.pe.kwonnam.slf4jlambda.defaultlogger.LambdaLoggerLocationAwareImpl#doLog
    // #4: kr.pe.kwonnam.slf4jlambda.LambdaLogger#debug
    // #5: com.virtuslab.logger.MacheteLogger#debug
    // #6: method from which IMacheteLogger.<logMethod> was called;
    var stackTrace = Thread.currentThread().getStackTrace();
    String methodName = Arrays.stream(stackTrace)
        .filter(se -> !isInternalLoggingClass(se.getClassName()))
        .findFirst()
        .map(se -> se.getMethodName())
        .orElse("<unknown>");
    String timerMessage = bufferedTimerMessage.get();
    bufferedTimerMessage.remove();
    return simpleClassName + "#" + methodName + ": " + (timerMessage == null ? "" : timerMessage);
  }

  private static String getStackTraceAsString(Throwable t) {
    StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }

  private static long getCurrentThreadId() {
    return Thread.currentThread().getId();
  }

  @Override
  public IEnhancedLambdaLogger startTimer() {
    bufferedTimerMessage.set("[thread #${getCurrentThreadId()}: starting timer] ");
    timerStartMillis.set(System.currentTimeMillis());
    return this;
  }

  @Override
  public IEnhancedLambdaLogger withTimeElapsed() {
    Long start = timerStartMillis.get();
    if (start == null) {
      throw new IllegalStateException("withTimeElapsed() called " +
          "without a preceding startTimer() call on thread #${getCurrentThreadId()}");
    }
    long elapsed = System.currentTimeMillis() - start;
    bufferedTimerMessage.set("[thread #${getCurrentThreadId()}: elapsed ${elapsed}ms] ");
    return this;
  }

  @Override
  public void trace(String format) {
    logger.trace(() -> getLogMessagePrefix() + format);
  }

  @Override
  public void trace(Supplier<String> msgSupplier) {
    logger.trace(() -> getLogMessagePrefix() + msgSupplier.get());
  }

  @Override
  public void debug(String format) {
    logger.debug(() -> getLogMessagePrefix() + format);
  }

  @Override
  public void debug(Supplier<String> msgSupplier) {
    logger.debug(() -> getLogMessagePrefix() + msgSupplier.get());
  }

  @Override
  public void info(String format) {
    logger.info(() -> getLogMessagePrefix() + format);
  }

  @Override
  public void info(Supplier<String> msgSupplier) {
    logger.info(() -> getLogMessagePrefix() + msgSupplier.get());
  }

  @Override
  public void warn(String format) {
    logger.warn(() -> getLogMessagePrefix() + format);
  }

  @Override
  public void error(String format) {
    logger.error(() -> getLogMessagePrefix() + format);
  }

  @Override
  public void error(String format, Throwable t) {
    logger.error(() -> getLogMessagePrefix() + format + System.lineSeparator() + getStackTraceAsString(t));
  }
}
