package com.virtuslab.gitmachete.frontend.file.grammar;

import com.intellij.lang.Language;

public class MacheteLanguage extends Language {
  public static final MacheteLanguage INSTANCE = new MacheteLanguage();

  protected MacheteLanguage() {
    super("Machete");
  }
}
