package bugdemo;

import org.checkerframework.checker.guieffect.qual.UIEffect;

public class ImplementingClass implements IDerivedInterface {
  @Override
  // This should cause a compilation error
  // `error: [[all, guieffect]:override.effect.invalid] A method override may only be @UI if it overrides an @UI method.`
  // but it doesn't...
  @UIEffect
  public void run() {}
  // Now try either extending BaseInterface directly instead of via DerivedInterface,
  // or adding a no-op override (`void run();`) to DerivedInterface -
  // and you'll get the compilation error, as expected.
}
