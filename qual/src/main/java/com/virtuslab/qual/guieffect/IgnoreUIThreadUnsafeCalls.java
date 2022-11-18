package com.virtuslab.qual.guieffect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark methods that get a free pass on calling {@link UIThreadUnsafe} methods,
 * despite <b>not</b> being marked as {@link UIThreadUnsafe} themselves.
 * <p>
 * This is only taken into account by ArchUnit test {@code com.virtuslab.archunit.UIThreadUnsafeMethodInvocationsTestSuite}
 * that enforces the correct usage of {@link UIThreadUnsafe} annotation.
 * <p>
 * Needs to be used sparingly, as this basically allows for a method to call potentially heavyweight operations on UI thread.
 * And that, in turn, might lead to UI freeze.
 * <p>
 * This annotation must have runtime retention to be visible to ArchUnit tests.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface IgnoreUIThreadUnsafeCalls {
  /**
   * {@code value} must include full names (in the format as returned by
   * {@code com.tngtech.archunit.core.domain.AccessTarget#getFullName()})
   * of UI-thread-unsafe methods that can legally be called from the annotated method.
   */
  String[] value();
}
