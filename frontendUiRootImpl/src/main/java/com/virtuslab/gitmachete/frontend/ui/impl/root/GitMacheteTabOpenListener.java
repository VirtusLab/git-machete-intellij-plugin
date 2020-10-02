package com.virtuslab.gitmachete.frontend.ui.impl.root;

import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

@CustomLog
public class GitMacheteTabOpenListener implements ContentManagerListener {

  @Override
  @UIEffect
  public void selectionChanged(ContentManagerEvent event) {
    if (isGitMacheteTabOpen(event)) {
      LOG.info("Performing on Git Machete Tab open action...");
      perform();
    }
  }

  private void perform() {
    System.out.println("hello there ~ called on open git machete tab");
  }

  private boolean isGitMacheteTabOpen(ContentManagerEvent event) {
    return event.getOperation().equals(ContentManagerEvent.ContentOperation.add) &&
            event.getContent().getDisplayName().equals("Git Machete");
  }
}
