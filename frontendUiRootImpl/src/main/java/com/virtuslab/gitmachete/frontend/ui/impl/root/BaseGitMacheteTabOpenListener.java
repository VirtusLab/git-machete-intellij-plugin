package com.virtuslab.gitmachete.frontend.ui.impl.root;

import com.intellij.openapi.project.Project;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

@CustomLog
@AllArgsConstructor
public abstract class BaseGitMacheteTabOpenListener implements ContentManagerListener {

  protected final Project project;

  @Override
  public void selectionChanged(ContentManagerEvent event) {
    if (isGitMacheteTabOpen(event)) {
      LOG.info("Performing on Git Machete Tab open action...");
      perform();
    }
  }

  public abstract void perform();

  private boolean isGitMacheteTabOpen(ContentManagerEvent event) {
    return event.getOperation() == ContentManagerEvent.ContentOperation.add &&
        event.getContent().getDisplayName().equals("Git Machete");
  }
}
