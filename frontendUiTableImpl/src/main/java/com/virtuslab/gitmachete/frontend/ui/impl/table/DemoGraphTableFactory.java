package com.virtuslab.gitmachete.frontend.ui.impl.table;

import com.intellij.ui.table.JBTable;

import com.virtuslab.gitmachete.frontend.ui.api.table.IDemoGraphTableFactory;

public class DemoGraphTableFactory implements IDemoGraphTableFactory {
  @Override
  public JBTable create() {
    return DemoGraphTable.INSTANCE;
  }
}
