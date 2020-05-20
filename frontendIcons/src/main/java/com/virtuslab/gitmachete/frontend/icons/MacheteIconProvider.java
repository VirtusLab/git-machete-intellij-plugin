package com.virtuslab.gitmachete.frontend.icons;

import javax.swing.Icon;

import com.intellij.openapi.externalSystem.ui.ExternalSystemIconProvider;
import icons.MacheteIcons;

public class MacheteIconProvider implements ExternalSystemIconProvider {
  @Override
  public Icon getReloadIcon() {
    return MacheteIcons.MACHETE_LOAD_CHANGES;
  }
}
