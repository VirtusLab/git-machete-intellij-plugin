package com.virtuslab.logger;

import java.util.function.Supplier;

public interface IPrefixedLambdaLogger {
  void debug(String format);
  void debug(Supplier<String> msgSupplier);
  void info(String format);
  void info(Supplier<String> msgSupplier);
  void warn(String format);
  void warn(Supplier<String> msgSupplier);
  void error(String format);
  void error(Supplier<String> msgSupplier);
}
