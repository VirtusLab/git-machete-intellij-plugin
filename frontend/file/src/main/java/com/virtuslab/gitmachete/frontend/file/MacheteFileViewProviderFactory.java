package com.virtuslab.gitmachete.frontend.file;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;

import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.FileViewProviderFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.util.FileContentUtilCore;
import com.intellij.util.ModalityUiUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;

public class MacheteFileViewProviderFactory implements FileViewProviderFactory {
  @Override
  public FileViewProvider createFileViewProvider(VirtualFile file,
      Language language,
      PsiManager manager,
      boolean eventSystemEnabled) {
    return new MacheteFileViewProvider(manager, file, eventSystemEnabled, language);
  }

  private static class MacheteFileViewProvider extends SingleRootFileViewProvider implements Disposable {

    MacheteFileViewProvider(PsiManager manager,
        VirtualFile virtualFile,
        boolean eventSystemEnabled,
        Language language) {
      super(manager, virtualFile, eventSystemEnabled, language);
      // TODO (#679): restore the subscription to git repo changes once we clarify why reparsing the files sometimes takes so long
      // subscribeToGitRepositoryChanges(manager.getProject(), language)
    }

    @Override
    protected boolean shouldCreatePsi() {
      return true;
    }

    private void subscribeToGitRepositoryChanges(Project project, Language language) {
      Topic<GitRepositoryChangeListener> topic = GitRepository.GIT_REPO_CHANGE;
      GitRepositoryChangeListener listener = repository -> ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> {
        PsiFile psiFile = getPsi(language);
        if (psiFile != null) {
          FileContentUtilCore.reparseFiles(psiFile.getVirtualFile());
        }
      });
      MessageBusConnection messageBusConnection = project.getMessageBus().connect();
      messageBusConnection.subscribe(topic, listener);
      Disposer.register(this, messageBusConnection);
    }

    @Override
    public void dispose() {

    }
  }

}
