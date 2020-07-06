package com.virtuslab.gitmachete.frontend.ui.impl.table;

import com.intellij.ui.table.JBTable;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.ui.api.table.IDemoGraphTableProvider;

public class DemoGraphTableProvider implements IDemoGraphTableProvider {
  @Override
  @UIEffect
  public JBTable getInstance() {
    return DemoGraphTable.deriveInstance();
  }
}
