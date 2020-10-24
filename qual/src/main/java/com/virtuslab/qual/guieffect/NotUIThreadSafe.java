package com.virtuslab.qual.guieffect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO (typetools/checker-framework#3252): replace with proper @Heavyweight
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
public @interface NotUIThreadSafe {}
