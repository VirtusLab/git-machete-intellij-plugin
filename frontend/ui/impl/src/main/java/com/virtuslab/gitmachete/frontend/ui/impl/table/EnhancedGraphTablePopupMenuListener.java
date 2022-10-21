package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;

import java.util.Timer;
import java.util.TimerTask;

import javax.swing.event.PopupMenuEvent;

import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.util.ModalityUiUtil;
import org.checkerframework.checker.guieffect.qual.UIEffect;

class EnhancedGraphTablePopupMenuListener extends PopupMenuListenerAdapter {
  private final EnhancedGraphTable graphTable;

  @UIEffect
  EnhancedGraphTablePopupMenuListener(EnhancedGraphTable graphTable) {
    this.graphTable = graphTable;
  }

  @Override
  @UIEffect
  public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
    // This delay is needed to avoid `focus transfer` effect when at the beginning row selection is light-blue,
    // but when the context menu is created (in a fraction of a second),
    // selection loses focus on the context menu and becomes dark blue.
    // TimerTask can't be replaced by lambda because it's not a SAM (single abstract method).
    // For more details see https://stackoverflow.com/a/37970821/10116324.
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> graphTable.setRowSelectionAllowed(true));
      }
    }, /* delay in ms */ 35);
  }

  @Override
  @UIEffect
  public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
    graphTable.setRowSelectionAllowed(false);
  }
}
