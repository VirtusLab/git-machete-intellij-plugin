package com.virtuslab.qual.gitmachete.frontend.graph.api.items;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;

import com.virtuslab.qual.internal.SubtypingTop;

/**
 * Used to annotate a type of a {@code com.virtuslab.gitmachete.frontend.graph.api.items.IBranchItem} object
 * that has been statically proven to be a {@code com.virtuslab.gitmachete.frontend.graph.impl.items.CommitItem}.
 * <p>
 * See <a href="https://checkerframework.org/manual/#subtyping-checker">Subtyping Checker manual</a>.
 * <p>
 * TODO (#859): consider switching from Subtyping Checker to {@code instanceof} once we migrate to Java 17.
 */
@Retention(RetentionPolicy.CLASS)
@SubtypeOf(SubtypingTop.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface ConfirmedCommitItem {}
