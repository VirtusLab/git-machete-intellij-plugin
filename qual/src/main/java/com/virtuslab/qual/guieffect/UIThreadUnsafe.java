package com.virtuslab.qual.guieffect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.checker.guieffect.qual.UIEffect;

/**
 * Used to mark methods that should NOT be executed on UI thread to avoid a UI freeze.
 * In case of IntelliJ Platform, the UI thread is Swing's Event Dispatch Thread aka EDT.
 * These include methods that are heavyweight or blocking. Typically, this means accessing disk and/or network.
 * <p>
 * It is <b>not</b> enforced during compilation whether the annotation is used correctly.
 * TODO (typetools/checker-framework#3253): replace with proper {@code @Heavyweight} annotation.
 * As for now, we use an ArchUnit test {@code com.virtuslab.archunit.UIThreadUnsafeMethodInvocationsTestSuite}
 * to enforce that:
 * <ol>
 * <li>methods that access certain known heavyweight methods (like the ones from {@code java.nio}) are marked as {@link UIThreadUnsafe}</li>
 * <li>methods that access {@link UIThreadUnsafe} methods are themselves marked as {@link UIThreadUnsafe}</li>
 * <li>no methods are marked as both {@link UIThreadUnsafe} and {@link UIEffect}</li>
 * </ol>
 * <p>
 * This annotation must have runtime retention to be visible to ArchUnit tests.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface UIThreadUnsafe {}
