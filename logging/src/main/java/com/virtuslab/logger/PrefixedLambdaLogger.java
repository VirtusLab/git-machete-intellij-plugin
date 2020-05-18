package com.virtuslab.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Supplier;

import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PrefixedLambdaLogger implements IPrefixedLambdaLogger {
  private final LambdaLogger logger;
  private final String className;
  private final ThreadLocal<@Nullable String> bufferedTimerMessage;
  private final ThreadLocal<@Nullable Long> timerStartMillis;

  PrefixedLambdaLogger(LambdaLogger logger) {
    this.logger = logger;
    // We are sure that at the moment when this constructor is called we have at least 4 elements in stacktrace array:
    // #0: java.lang.Thread#getStackTrace
    // #1: com.virtuslab.logger.MacheteLogger#<init>
    // #2: com.virtuslab.logger.MacheteLoggerFactory#getLogger
    // #3: method from which MacheteLoggerFactory#getLogger was called
    @SuppressWarnings("upperbound") StackTraceElement element = Thread.currentThread().getStackTrace()[3];
    String[] classPath = element.getClassName().split("\\.");
    this.className = classPath[classPath.length - 1];
    this.bufferedTimerMessage = new ThreadLocal<>();
    this.timerStartMillis = new ThreadLocal<>();
  }

  private String getLogMessagePrefix() {
    // We are sure that at the moment when this method is called we have at least 7 elements in stacktrace array:
    // #0: java.lang.Thread#getStackTrace
    // #1: com.virtuslab.logger.MacheteLogger#getMethodReferenceName
    // #2: com.virtuslab.logger.MacheteLogger#lambda$debug$0
    // #3: kr.pe.kwonnam.slf4jlambda.defaultlogger.LambdaLoggerLocationAwareImpl#doLog
    // #4: kr.pe.kwonnam.slf4jlambda.LambdaLogger#debug
    // #5: com.virtuslab.logger.MacheteLogger#debug
    // #6: method from which IMacheteLogger.<logMethod> was called;
    @SuppressWarnings("upperbound") StackTraceElement element = Thread.currentThread().getStackTrace()[6];
    String timerMessage = bufferedTimerMessage.get();
    bufferedTimerMessage.remove();
    return className + "#" + element.getMethodName() + ": " + (timerMessage == null ? "" : timerMessage);
  }

  private String getStackTraceAsString(Throwable t) {
    StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }

  private long getCurrentThreadId() {
    return Thread.currentThread().getId();
  }

  @Override
  public IPrefixedLambdaLogger startTimer() {
    bufferedTimerMessage.set("[thread #${getCurrentThreadId()}: starting timer] ");
    timerStartMillis.set(System.currentTimeMillis());
    return this;
  }

  @Override
  public IPrefixedLambdaLogger withTimeElapsed() {
    Long start = timerStartMillis.get();
    if (start == null) {
      throw new IllegalStateException("withTimeElapsed() called without a preceding startTimer() call");
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
