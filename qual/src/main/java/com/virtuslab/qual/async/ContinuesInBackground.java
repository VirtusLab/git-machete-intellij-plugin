package com.virtuslab.qual.async;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark methods that schedule a background task.
 * It is <b>not</b> enforced during compilation whether the annotation is used correctly.
 * Its purpose is to make the programmer aware of the increased risk of race conditions,
 * esp. to avoid treating such methods as if they completed in a fully synchronous/blocking manner.
 * <p>
 * As for now, we use an ArchUnit test {@code com.virtuslab.archunit.BackgroundTaskEnqueuingTestSuite}
 * to enforce that:
 * <ol>
 * <li>methods that call {@code queue()} on classes extending {@code Task.Backgroundable} are marked as {@link ContinuesInBackground}</li>
 * <li>methods that call {@code git4idea.branch.GitBrancher#checkout} are marked as {@link ContinuesInBackground}</li>
 * <li>methods that call {@link ContinuesInBackground} methods are themselves marked as {@link ContinuesInBackground}</li>
 * </ol>
 * <p>
 * This annotation must have runtime retention to be visible to ArchUnit tests.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ContinuesInBackground {}
