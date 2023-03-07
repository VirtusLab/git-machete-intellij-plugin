package com.virtuslab.qual.subtyping.gitmachete.frontend.graph.api.elements;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;

import com.virtuslab.qual.subtyping.internal.SubtypingTop;

/**
 * Used to annotate a type of a {@code com.virtuslab.gitmachete.frontend.graph.api.elements.IGraphElement} object
 * that has been statically proven to be a {@code com.virtuslab.gitmachete.frontend.graph.api.elements.GraphEdge}.
 * <p>
 * See <a href="https://checkerframework.org/manual/#subtyping-checker">Subtyping Checker manual</a>.
 */
@Retention(RetentionPolicy.CLASS)
@SubtypeOf(SubtypingTop.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface ConfirmedGraphEdge {}
