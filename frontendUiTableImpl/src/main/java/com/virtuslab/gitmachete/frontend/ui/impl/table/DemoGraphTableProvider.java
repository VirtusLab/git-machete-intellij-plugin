package com.virtuslab.gitmachete.frontend.ui.impl.table;

import com.intellij.ui.table.JBTable;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.ui.api.table.IDemoGraphTableProvider;

public class DemoGraphTableProvider implements IDemoGraphTableProvider {
  @Override
  @UIEffect
  public JBTable deriveInstance() {
    // The reinstantiation is needed every time because without it
    // the table keeps the first IDE theme despite the theme changes.
    return DemoGraphTable.deriveInstance();
  }
}
