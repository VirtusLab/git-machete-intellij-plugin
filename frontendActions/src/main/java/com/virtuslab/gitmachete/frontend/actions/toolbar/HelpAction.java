package com.virtuslab.gitmachete.frontend.actions.toolbar;

import javax.swing.Action;
import javax.swing.JComponent;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.frontend.ui.api.table.IDemoGraphTableProvider;

public class HelpAction extends DumbAwareAction {

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent e) {
    new HelpDialog().show();
  }

  @UI
  private static final class HelpDialog extends DialogWrapper {
    private static final int CENTER_PANEL_HEIGHT = 250;
    private static final int CENTER_PANEL_WIDTH = 800;

    HelpDialog() {
      super(/* canBeParent */ false);

      // Note: since the class is final, `this` is already @Initialized at this point.
      init();
      setTitle("Git Machete Help");
    }

    @Override
    @SuppressWarnings("interning:not.interned") // to allow for `helpAction == myHelpAction`
    protected Action[] createActions() {
      Action helpAction = getHelpAction();
      return helpAction == myHelpAction && getHelpId() == null
          ? new Action[]{getOKAction()}
          : new Action[]{getOKAction(), helpAction};
    }

    @Override
    protected JComponent createCenterPanel() {
      var panel = JBUI.Panels.simplePanel(/* hgap */ 0, /* vgap */ 2);
      var demoGraphTable = RuntimeBinding.instantiateSoleImplementingClass(IDemoGraphTableProvider.class).getInstance();
      panel.addToCenter(ScrollPaneFactory.createScrollPane(demoGraphTable));
      panel.setPreferredSize(new JBDimension(CENTER_PANEL_WIDTH, CENTER_PANEL_HEIGHT));
      return panel;
    }
  }

}
