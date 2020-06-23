package com.virtuslab.gitmachete.frontend.ui.impl.table;

import com.intellij.ui.table.JBTable;

import com.virtuslab.gitmachete.frontend.ui.api.table.IDemoGraphTableProvider;

public class DemoGraphTableProvider implements IDemoGraphTableProvider {
  @Override
  public JBTable getInstance() {
    return DemoGraphTable.INSTANCE;
  }
}
