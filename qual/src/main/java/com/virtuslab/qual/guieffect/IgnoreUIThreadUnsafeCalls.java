package com.virtuslab.qual.guieffect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark methods that get a free pass on calling {@link UIThreadUnsafe} methods,
 * despite NOT being marked as {@link UIThreadUnsafe} themselves.
 * <p>
 * This is only taken into account by ArchUnit test {@code com.virtuslab.archunit.UIThreadUnsafeMethodInvocationsTestSuite}
 * that enforces the correct usage of {@link UIThreadUnsafe} annotation.
 * <p>
 * Needs to be used sparingly, as this basically allows for a method to call potentially heavyweight operations on UI thread,
 * which might lead to UI freeze.
 * <p>
 * This annotation must have runtime retention to be visible to ArchUnit tests.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface IgnoreUIThreadUnsafeCalls {}
