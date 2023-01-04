package com.virtuslab.qual.async;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark methods that get a free pass on calling {@link ContinuesInBackground} methods,
 * despite <b>not</b> being marked as {@link ContinuesInBackground} themselves.
 * <p>
 * This is only taken into account by ArchUnit test {@code com.virtuslab.archunit.BackgroundTaskEnqueuingTestSuite}
 * that enforces the correct usage of {@link ContinuesInBackground} annotation.
 * <p>
 * This annotation must have runtime retention to be visible to ArchUnit tests.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface DoesNotContinueInBackground {
  String reason();
}
