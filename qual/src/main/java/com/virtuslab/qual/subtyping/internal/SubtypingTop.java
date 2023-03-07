package com.virtuslab.qual.subtyping.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * There needs to be single subtyping hierarchy with single bottom and top annotation.
 * We could theoretically create a separate hierarchy with a dedicated top and bottom type
 * for each pair of annotations from {@link com.virtuslab.qual.subtyping}.* packages,
 * but then <a href="https://checkerframework.org/manual/#subtyping-checker">Subtyping Checker</a>
 * would raise an error about multiple top/bottom types.
 */
@DefaultQualifierInHierarchy
@Retention(RetentionPolicy.CLASS)
@SubtypeOf({})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SubtypingTop {}
