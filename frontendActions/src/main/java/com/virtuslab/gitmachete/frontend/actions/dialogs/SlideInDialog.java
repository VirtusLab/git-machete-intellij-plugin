package com.virtuslab.gitmachete.frontend.actions.dialogs;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogPanel;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBTextField;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.common.GitMacheteBundle;

public final class SlideInDialog extends DialogWrapper {

  private static final int COMPONENT_WIDTH = 360;

  private final JBTextField textField = new JBTextField();

  @UIEffect
  public SlideInDialog(Project project, String parentName) {
    super(project);
    setTitle(GitMacheteBundle.message("action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.title", parentName));

    init();
    setOKButtonText(GitMacheteBundle.message("action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.ok-button"));
    setOKButtonMnemonic('I');
  }

  @Override
  @UIEffect
  protected JComponent createCenterPanel() {
    var label = new JLabel(GitMacheteBundle.message("action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.label"));
    var panel = new DialogPanel();
    panel.setLayout(new GridLayout(/* rows */ 2, /* cols */ 1));
    panel.setPreferredSize(new Dimension(COMPONENT_WIDTH, textField.getHeight()));
    panel.add(row(label));
    panel.add(row(textField));
    panel.setPreferredFocusedComponent(textField);
    return panel;
  }

  @Nullable
  @UIEffect
  public String showAndGetEntryName() {
    return showAndGet() ? textField.getText().trim() : null;
  }

  @UIEffect
  private static JComponent row(JComponent component) {
    component.setLayout(new BoxLayout(component, BoxLayout.X_AXIS));
    return component;
  }

}
