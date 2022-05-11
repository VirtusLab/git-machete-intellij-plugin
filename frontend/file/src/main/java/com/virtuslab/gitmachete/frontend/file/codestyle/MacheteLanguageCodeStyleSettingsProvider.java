package com.virtuslab.gitmachete.frontend.file.codestyle;

import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.application.options.SmartIndentOptionsEditor;
import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;

import com.virtuslab.gitmachete.frontend.file.MacheteFileUtils;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteLanguage;

public class MacheteLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {

  @Override
  public Language getLanguage() {
    return MacheteLanguage.instance;
  }

  @Override
  protected void customizeDefaults(CommonCodeStyleSettings commonSettings,
      CommonCodeStyleSettings.IndentOptions indentOptions) {

    indentOptions.USE_TAB_CHARACTER = true;
    indentOptions.TAB_SIZE = 4;
    indentOptions.INDENT_SIZE = indentOptions.TAB_SIZE;
    indentOptions.KEEP_INDENTS_ON_EMPTY_LINES = false;

    commonSettings.KEEP_BLANK_LINES_IN_CODE = 1;
  }

  @Override
  public void customizeSettings(CodeStyleSettingsCustomizable consumer, SettingsType settingsType) {
    if (settingsType == SettingsType.INDENT_SETTINGS) {
      consumer.showStandardOptions("USE_TAB_CHARACTER", "TAB_SIZE", "INDENT_SIZE", "KEEP_INDENTS_ON_EMPTY_LINES");
    } else if (settingsType == SettingsType.BLANK_LINES_SETTINGS) {
      consumer.showStandardOptions("KEEP_BLANK_LINES_IN_CODE");
    }
  }

  @Override
  public IndentOptionsEditor getIndentOptionsEditor() {
    return new SmartIndentOptionsEditor(this);
  }

  @Override
  public String getCodeSample(SettingsType settingsType) {
    return MacheteFileUtils.getSampleMacheteFileContents();
  }
}
