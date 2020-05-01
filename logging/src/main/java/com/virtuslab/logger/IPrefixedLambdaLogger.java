package com.virtuslab.logger;

import java.util.function.Supplier;

public interface IPrefixedLambdaLogger {
  void trace(String format);
  void trace(Supplier<String> msgSupplier);
  void debug(String format);
  void debug(Supplier<String> msgSupplier);
  void info(String format);
  void warn(String format);
  void error(String format);
  void error(String format, Throwable t);
}
