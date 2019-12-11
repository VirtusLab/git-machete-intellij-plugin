package com.virtuslab.gitmachete;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.virtuslab.gitmachete.backendroot.GitFactoryModule;
import com.virtuslab.gitmachete.data.RepositoryGraphImpl;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.ui.table.GitMacheteGraphTable;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;

public class GitMacheteContentProvider implements ToolWindowFactory {

  @Override
  public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
    // todo: enable this line and fix javafx import
    //    Platform.setImplicitExit(false);

    IGitMacheteRepository repository = null;

    try {
      GitMacheteRepositoryFactory instance =
          GitFactoryModule.getInjector().getInstance(GitMacheteRepositoryFactory.class);
      repository =
          instance.create(
              Paths.get(Objects.requireNonNull(project.getBasePath())), Optional.empty());

    } catch (GitMacheteException e) {
      e.printStackTrace();
    }

    GitMacheteGraphTable gitMacheteGraphTable =
        new GitMacheteGraphTable(new RepositoryGraphImpl(repository));
    toolWindow.getComponent().getParent().add(gitMacheteGraphTable);
  }
}
