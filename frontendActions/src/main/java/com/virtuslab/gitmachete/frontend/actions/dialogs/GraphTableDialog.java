package com.virtuslab.gitmachete.frontend.actions.dialogs;

import javax.swing.Action;
import javax.swing.JComponent;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import org.checkerframework.checker.guieffect.qual.UI;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.ui.api.table.ISimpleGraphTableProvider;

@UI
public final class GraphTableDialog extends DialogWrapper {
  private static final int CENTER_PANEL_HEIGHT = 250;
  private static final int CENTER_PANEL_WIDTH = 800;
  private final JBTable table;

  public static GraphTableDialog of(IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot, String windowTitle) {
    var table = RuntimeBinding.instantiateSoleImplementingClass(ISimpleGraphTableProvider.class)
        .deriveInstance(gitMacheteRepositorySnapshot);
    return new GraphTableDialog(table, windowTitle);
  }

  public static GraphTableDialog ofDemoRepository() {
    var table = RuntimeBinding.instantiateSoleImplementingClass(ISimpleGraphTableProvider.class).deriveDemoInstance();
    return new GraphTableDialog(table, "Git Machete Help");
  }

  private GraphTableDialog(JBTable table, String windowTitle) {
    super(/* canBeParent */ false);

    this.table = table;

    // Note: since the class is final, `this` is already @Initialized at this point.
    init();
    setTitle(windowTitle);
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
    panel.addToCenter(ScrollPaneFactory.createScrollPane(table));
    panel.setPreferredSize(new JBDimension(CENTER_PANEL_WIDTH, CENTER_PANEL_HEIGHT));
    return panel;
  }
}
