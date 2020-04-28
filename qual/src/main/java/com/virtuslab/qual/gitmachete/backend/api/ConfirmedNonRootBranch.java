package com.virtuslab.qual.gitmachete.backend.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;

import com.virtuslab.qual.internal.SubtypingTop;

@SubtypeOf(SubtypingTop.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface ConfirmedNonRootBranch {}
