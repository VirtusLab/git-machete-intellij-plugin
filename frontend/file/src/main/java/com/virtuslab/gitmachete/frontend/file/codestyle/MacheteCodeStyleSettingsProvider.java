package com.virtuslab.gitmachete.frontend.file.codestyle;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.psi.codeStyle.CodeStyleConfigurable;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.file.grammar.MacheteLanguage;

public class MacheteCodeStyleSettingsProvider extends CodeStyleSettingsProvider {
  public CodeStyleConfigurable createConfigurable(CodeStyleSettings settings, CodeStyleSettings modelSettings) {
    return new CodeStyleAbstractConfigurable(settings, modelSettings, this.getConfigurableDisplayName()) {
      @Override
      protected CodeStyleAbstractPanel createPanel(CodeStyleSettings settings) {
        return new MacheteCodeStyleMainPanel(getCurrentSettings(), settings);
      }
    };
  }

  @Override
  public String getConfigurableDisplayName() {
    return MacheteLanguage.instance.getDisplayName();
  }

  private static class MacheteCodeStyleMainPanel extends TabbedLanguageCodeStylePanel {
    @UIEffect
    MacheteCodeStyleMainPanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
      super(MacheteLanguage.instance, currentSettings, settings);
    }

    @Override
    @UIEffect
    protected void initTabs(CodeStyleSettings settings) {
      if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
        var sw = new java.io.StringWriter();
        var pw = new java.io.PrintWriter(sw);
        new Exception().printStackTrace(pw);
        String stackTrace = sw.toString();
        System.out.println("Expected EDT:");
        System.out.println(stackTrace);
        throw new RuntimeException("Expected EDT: " + stackTrace);
      }
      addIndentOptionsTab(settings);
      addBlankLinesTab(settings);
    }
  }
}
