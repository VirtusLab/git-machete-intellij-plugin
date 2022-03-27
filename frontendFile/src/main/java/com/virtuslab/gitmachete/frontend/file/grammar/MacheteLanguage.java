package com.virtuslab.gitmachete.frontend.file.grammar;

import com.intellij.lang.Language;

public final class MacheteLanguage extends Language {
  public static final MacheteLanguage instance = new MacheteLanguage();

  private MacheteLanguage() {
    super("Git Machete");
  }
}
