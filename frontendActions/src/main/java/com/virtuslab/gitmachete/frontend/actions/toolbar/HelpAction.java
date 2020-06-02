package com.virtuslab.gitmachete.frontend.actions.toolbar;

import javax.swing.JComponent;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.ui.impl.demo.DemoGraphTable;

public class HelpAction extends DumbAwareAction {
  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent e) {
    new HelpDialog().show();
  }

  @UI
  private final class HelpDialog extends DialogWrapper {
    private static final int CENTER_PANEL_HEIGHT = 250;
    private static final int CENTER_PANEL_WIDTH = 800;

    protected HelpDialog() {
      super(/* canBeParent */ false);
      init();
      setTitle("Git Machete Help");
    }

    @Override
    protected JComponent createCenterPanel() {
      var panel = JBUI.Panels.simplePanel(0, 2);
      panel.addToCenter(ScrollPaneFactory.createScrollPane(DemoGraphTable.INSTANCE));
      panel.setPreferredSize(new JBDimension(CENTER_PANEL_WIDTH, CENTER_PANEL_HEIGHT));
      return panel;
    }
  }

}
