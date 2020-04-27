package com.virtuslab.logger;

import java.util.function.Supplier;

public interface IMacheteLogger {
  void debug(String format);
  void debug(Supplier<String> msgSupplier);
}
