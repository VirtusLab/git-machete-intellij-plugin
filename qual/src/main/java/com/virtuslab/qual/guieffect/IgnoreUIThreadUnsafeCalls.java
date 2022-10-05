package com.virtuslab.qual.guieffect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Must have runtime retention to be visible to ArchUnit tests.
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface IgnoreUIThreadUnsafeCalls {}
