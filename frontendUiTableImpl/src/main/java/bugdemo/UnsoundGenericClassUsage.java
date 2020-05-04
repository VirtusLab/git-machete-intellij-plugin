package bugdemo;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

class UnsoundGenericClassUsage {

  <T> GenericClass<T> bar(GenericClass<T> key, T value) {
    return key;
  }

  <T> void qux(GenericClass<T> key) {}

  void foo() {
    GenericClass<@NonNull String> x = new GenericClass<>();

    // bar(x, null); // does not compile, okay
    bar((GenericClass<@Nullable String>) x, null); // compiles, okay
    // bar((GenericClass<@NonNull String>) x, null); // does not compile, okay

    // qux(this.<@NonNull String>bar(x, null)); // does not compile, okay
    // qux(this.<@Nullable String>bar(x, null)); // does not compile, okay
    qux(bar(x, null)); // COMPILES BUT SHOULDN'T (?)

    bar((GenericClass<@Nullable String>) x, null); // compiles, okay
    // bar((GenericClass<@NonNull String>) x, null); // does not compile, okay

    qux(bar((GenericClass<@Nullable String>) x, null)); // compiles, okay
    qux(bar((GenericClass<@NonNull String>) x, null)); // COMPILES BUT SHOULDN'T (?)

    this.<@Nullable String>qux(bar(x, null)); // compiles, okay
    this.<@NonNull String>qux(bar(x, null)); // COMPILES BUT SHOULDN'T (?)
  }
}
