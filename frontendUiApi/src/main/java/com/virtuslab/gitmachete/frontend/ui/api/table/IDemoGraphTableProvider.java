package com.virtuslab.gitmachete.frontend.ui.api.table;

import com.intellij.ui.table.JBTable;
import org.checkerframework.checker.guieffect.qual.UIEffect;

public interface IDemoGraphTableProvider {
  @UIEffect
  JBTable deriveInstance();
}
