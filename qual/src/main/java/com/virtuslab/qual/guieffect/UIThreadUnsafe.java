package com.virtuslab.qual.guieffect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO (typetools/checker-framework#3252): replace with proper @Heavyweight

// Must have runtime retention to be visible to ArchUnit tests.
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface UIThreadUnsafe {}
