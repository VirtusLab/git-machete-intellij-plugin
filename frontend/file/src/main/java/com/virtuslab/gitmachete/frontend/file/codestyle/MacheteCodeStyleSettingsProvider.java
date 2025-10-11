package com.virtuslab.gitmachete.frontend.file.codestyle;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleConfigurable;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.file.grammar.MacheteLanguage;

public class MacheteCodeStyleSettingsProvider extends CodeStyleSettingsProvider {
  @Override
  public CodeStyleConfigurable createConfigurable(CodeStyleSettings settings, CodeStyleSettings modelSettings) {
    return new CodeStyleAbstractConfigurable(settings, modelSettings, this.getConfigurableDisplayName()) {
      @Override
      protected CodeStyleAbstractPanel createPanel(CodeStyleSettings settings) {
        return new MacheteCodeStyleMainPanel(getCurrentSettings(), settings);
      }
    };
  }

  @Override
  public Language getLanguage() {
    return MacheteLanguage.instance;
  }

  @Override
  public String getConfigurableDisplayName() {
    return getLanguage().getDisplayName();
  }

  private static class MacheteCodeStyleMainPanel extends TabbedLanguageCodeStylePanel {
    @UIEffect
    MacheteCodeStyleMainPanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
      super(MacheteLanguage.instance, currentSettings, settings);
    }

    @Override
    @UIEffect
    protected void initTabs(CodeStyleSettings settings) {
      addIndentOptionsTab(settings);
      addBlankLinesTab(settings);
    }
  }
}
