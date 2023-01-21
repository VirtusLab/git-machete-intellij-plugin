package com.virtuslab.qual.async;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark methods that create an instance of {@code com.intellij.openapi.progress.Task.Backgroundable},
 * but do <b>not</b> call {@code #queue()}.
 * <p>
 * Typically, it is a smell that indicates that the programmer forgot to actually schedule the task.
 * Such cases are detected by ArchUnit test {@code com.virtuslab.archunit.BackgroundTaskEnqueuingTestSuite}.
 * Still, in some rare cases it might happen that a method passes the newly-created backgroundable
 * to downstream APIs to actually enqueue.
 * Such methods must be marked as {@link BackgroundableQueuedElsewhere} for {@code BackgroundTaskEnqueuingTestSuite} to ignore.
 * <p>
 * This annotation must have runtime retention to be visible to ArchUnit tests.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface BackgroundableQueuedElsewhere {}
