package com.virtuslab.gitmachete.frontend.file;

import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;

public class MacheteLanguage extends Language {
    public static final MacheteLanguage INSTANCE = new MacheteLanguage();

    protected MacheteLanguage() {
        super("Machete");
    }
}
