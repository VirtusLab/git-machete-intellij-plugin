package com.virtuslab.archunit;

import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.codeUnits;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noCodeUnits;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import java.util.Arrays;

import com.intellij.openapi.progress.Task;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import io.vavr.collection.List;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.junit.jupiter.api.Test;

import com.virtuslab.qual.guieffect.IgnoreUIThreadUnsafeCalls;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(Arrays.class)
public class UIThreadUnsafeMethodInvocationsTestSuite extends BaseArchUnitTestSuite {

  private static final String UIThreadUnsafeName = "@" + UIThreadUnsafe.class.getSimpleName();

  @Test
  public void task_backgroundable_run_methods_must_be_ui_thread_unsafe() {
    methods()
        .that()
        .haveNameMatching("^(doRun|run)$")
        .and()
        .areDeclaredInClassesThat()
        .areAssignableTo(Task.Backgroundable.class)
        .should()
        .beAnnotatedWith(UIThreadUnsafe.class)
        .because("it probably doesn't make sense to extract a backgroundable task " +
            "for actions that can as well be executed on UI thread")
        .check(productionClasses);
  }

  @Test
  public void ui_thread_unsafe_code_units_should_not_be_uieffect() {
    codeUnits()
        .that()
        .areAnnotatedWith(UIThreadUnsafe.class)
        .should()
        .notBeAnnotatedWith(UIEffect.class)
        .check(productionClasses);
  }

  private static List<String> extractWhitelistedCodeUnitsFromAnnotation(JavaCodeUnit codeUnit) {
    if (codeUnit.isAnnotatedWith(IgnoreUIThreadUnsafeCalls.class)) {
      return List.of(codeUnit.getAnnotationOfType(IgnoreUIThreadUnsafeCalls.class).value());
    } else {
      return List.empty();
    }
  }

  @Test
  public void only_ui_thread_unsafe_code_units_should_call_other_ui_thread_unsafe_code_units() {
    noCodeUnits()
        .that()
        .areNotAnnotatedWith(UIThreadUnsafe.class)
        .and()
        // As of early 2023, the only place where such methods are generated are the calls of `Try.of(...)`,
        // which require a parameter of type `CheckedFunction0`, which implements `Serializable`.
        // See https://www.baeldung.com/java-serialize-lambda for details.
        .doNotHaveName("$deserializeLambda$")
        .should(callAnyCodeUnitsThat("are UI thread unsafe",
            (codeUnit, calledCodeUnit) -> isUIThreadUnsafe(calledCodeUnit)
                && !extractWhitelistedCodeUnitsFromAnnotation(codeUnit).contains(calledCodeUnit.getFullName())))
        .check(productionClasses);
  }

  private static final String[] knownBlockingCodeUnits = {
      "com.intellij.dvcs.push.PushController.push(boolean)",
      "com.intellij.openapi.vcs.changes.VcsFreezingProcess.execute()",
      "java.lang.Process.waitFor(long, java.util.concurrent.TimeUnit)",
      "java.lang.Thread.sleep(long)",
  };

  private static final String[] uiThreadUnsafePackagePrefixes = {
      "git4idea",
      "java.io",
      "java.nio",
      "org.eclipse.jgit",
  };

  private static final String[] uiThreadSafeCodeUnitsInUnsafePackages = {
      "git4idea.GitLocalBranch.getName()",
      "git4idea.GitRemoteBranch.getName()",
      "git4idea.GitRemoteBranch.getNameForRemoteOperations()",
      "git4idea.GitUtil.findGitDir(com.intellij.openapi.vfs.VirtualFile)",
      "git4idea.GitUtil.getRepositories(com.intellij.openapi.project.Project)",
      "git4idea.GitUtil.getRepositoryManager(com.intellij.openapi.project.Project)",
      "git4idea.branch.GitBranchUtil.sortBranchNames(java.util.Collection)",
      "git4idea.branch.GitBranchOperationType.getDescription()",
      "git4idea.branch.GitBranchOperationType.getText()",
      "git4idea.branch.GitBrancher.checkout(java.lang.String, boolean, java.util.List, java.lang.Runnable)",
      "git4idea.branch.GitBrancher.compareAny(java.lang.String, java.lang.String, java.util.List)",
      "git4idea.branch.GitBrancher.createBranch(java.lang.String, java.util.Map)",
      "git4idea.branch.GitBrancher.deleteBranch(java.lang.String, java.util.List)",
      "git4idea.branch.GitBrancher.deleteBranches(java.util.Map, java.lang.Runnable)",
      "git4idea.branch.GitBrancher.getInstance(com.intellij.openapi.project.Project)",
      "git4idea.branch.GitBrancher.merge(java.lang.String, git4idea.branch.GitBrancher$DeleteOnMergeOption, java.util.List)",
      "git4idea.branch.GitBrancher.renameBranch(java.lang.String, java.lang.String, java.util.List)",
      "git4idea.branch.GitBranchesCollection.findBranchByName(java.lang.String)",
      "git4idea.branch.GitBranchesCollection.findLocalBranch(java.lang.String)",
      "git4idea.branch.GitBranchesCollection.findRemoteBranch(java.lang.String)",
      "git4idea.branch.GitBranchesCollection.getHash(git4idea.GitBranch)",
      "git4idea.branch.GitBranchesCollection.getLocalBranches()",
      "git4idea.branch.GitBranchesCollection.getRemoteBranches()",
      "git4idea.branch.GitNewBranchDialog.<init>(com.intellij.openapi.project.Project, java.util.Collection, java.lang.String, java.lang.String, boolean, boolean, boolean)",
      "git4idea.branch.GitNewBranchDialog.<init>(com.intellij.openapi.project.Project, java.util.Collection, java.lang.String, java.lang.String, boolean, boolean, boolean, boolean, git4idea.branch.GitBranchOperationType)",
      "git4idea.branch.GitNewBranchDialog.showAndGetOptions()",
      "git4idea.branch.GitNewBranchOptions.getName()",
      "git4idea.branch.GitNewBranchOptions.shouldCheckout()",
      "git4idea.config.GitConfigUtil.getBooleanValue(java.lang.String)",
      "git4idea.config.GitSharedSettings.getInstance(com.intellij.openapi.project.Project)",
      "git4idea.config.GitSharedSettings.isBranchProtected(java.lang.String)",
      "git4idea.config.GitVcsSettings.getInstance(com.intellij.openapi.project.Project)",
      "git4idea.config.GitVcsSettings.getRecentRootPath()",
      "git4idea.fetch.GitFetchResult.showNotification()",
      "git4idea.fetch.GitFetchSupport.fetchSupport(com.intellij.openapi.project.Project)",
      "git4idea.fetch.GitFetchSupport.isFetchRunning()",
      "git4idea.push.GitPushSource.create(git4idea.GitLocalBranch)",
      "git4idea.repo.GitRemote.getName()",
      "git4idea.repo.GitRepository.getBranches()",
      "git4idea.repo.GitRepository.getCurrentBranch()",
      "git4idea.repo.GitRepository.getCurrentBranchName()",
      "git4idea.repo.GitRepository.getProject()",
      "git4idea.repo.GitRepository.getRemotes()",
      "git4idea.repo.GitRepository.getRoot()",
      "git4idea.repo.GitRepository.getState()",
      "git4idea.repo.GitRepository.getVcs()",
      "git4idea.ui.ComboBoxWithAutoCompletion.<init>(javax.swing.ComboBoxModel, com.intellij.openapi.project.Project)",
      "git4idea.ui.ComboBoxWithAutoCompletion.addDocumentListener(com.intellij.openapi.editor.event.DocumentListener)",
      "git4idea.ui.ComboBoxWithAutoCompletion.getModel()",
      "git4idea.ui.ComboBoxWithAutoCompletion.getText()",
      "git4idea.ui.ComboBoxWithAutoCompletion.selectAll()",
      "git4idea.ui.ComboBoxWithAutoCompletion.setPlaceholder(java.lang.String)",
      "git4idea.ui.ComboBoxWithAutoCompletion.setPrototypeDisplayValue(java.lang.Object)",
      "git4idea.ui.ComboBoxWithAutoCompletion.setUI(javax.swing.plaf.ComboBoxUI)",
      "git4idea.ui.branch.GitBranchCheckoutOperation.<init>(com.intellij.openapi.project.Project, java.util.Collection)",
      "git4idea.validators.GitBranchValidatorKt.checkRefName(java.lang.String)",
      "git4idea.validators.GitBranchValidatorKt.checkRefNameEmptyOrHead(java.lang.String)",
      "git4idea.validators.GitBranchValidatorKt.conflictsWithLocalBranch(java.util.Collection, java.lang.String)",
      "git4idea.validators.GitBranchValidatorKt.conflictsWithLocalBranchDirectory(java.util.Set, java.lang.String)",
      "git4idea.validators.GitBranchValidatorKt.conflictsWithRemoteBranch(java.util.Collection, java.lang.String)",
      "git4idea.validators.GitRefNameValidator.cleanUpBranchName(java.lang.String)",
      "git4idea.validators.GitRefNameValidator.cleanUpBranchNameOnTyping(java.lang.String)",
      "git4idea.validators.GitRefNameValidator.getInstance()",
      // Some of these java.(n)io methods might actually access the filesystem;
      // still, they're lightweight enough so that we can give them a free pass.
      "java.io.BufferedOutputStream.<init>(java.io.OutputStream)",
      "java.io.File.canExecute()",
      "java.io.File.getAbsolutePath()",
      "java.io.File.isFile()",
      "java.io.File.toString()",
      "java.io.IOException.getMessage()",
      "java.nio.file.Files.isRegularFile(java.nio.file.Path, [Ljava.nio.file.LinkOption;)",
      "java.nio.file.Files.readAttributes(java.nio.file.Path, java.lang.Class, [Ljava.nio.file.LinkOption;)",
      "java.nio.file.Files.setLastModifiedTime(java.nio.file.Path, java.nio.file.attribute.FileTime)",
      "java.nio.file.Path.equals(java.lang.Object)",
      "java.nio.file.Path.getFileName()",
      "java.nio.file.Path.getParent()",
      "java.nio.file.Path.of(java.lang.String, [Ljava.lang.String;)",
      "java.nio.file.Path.resolve(java.lang.String)",
      "java.nio.file.Path.toAbsolutePath()",
      "java.nio.file.Path.toFile()",
      "java.nio.file.Path.toString()",
      "java.nio.file.Paths.get(java.lang.String, [Ljava.lang.String;)",
      "java.nio.file.attribute.BasicFileAttributes.lastModifiedTime()",
      "java.nio.file.attribute.FileTime.fromMillis(long)",
      "java.nio.file.attribute.FileTime.toMillis()",
      "org.eclipse.jgit.lib.CheckoutEntry.getFromBranch()",
      "org.eclipse.jgit.lib.CheckoutEntry.getToBranch()",
      "org.eclipse.jgit.lib.ObjectId.equals(org.eclipse.jgit.lib.AnyObjectId)",
      "org.eclipse.jgit.lib.ObjectId.getName()",
      "org.eclipse.jgit.lib.ObjectId.zeroId()",
      "org.eclipse.jgit.lib.PersonIdent.getWhen()",
      "org.eclipse.jgit.lib.Ref.getName()",
      "org.eclipse.jgit.lib.Ref.getObjectId()",
      "org.eclipse.jgit.lib.ReflogEntry.getComment()",
      "org.eclipse.jgit.lib.ReflogEntry.getNewId()",
      "org.eclipse.jgit.lib.ReflogEntry.getOldId()",
      "org.eclipse.jgit.lib.ReflogEntry.getWho()",
      "org.eclipse.jgit.lib.ReflogEntry.parseCheckout()",
      "org.eclipse.jgit.lib.RepositoryState.ordinal()",
      "org.eclipse.jgit.lib.RepositoryState.values()",
      "org.eclipse.jgit.revwalk.RevCommit.getCommitTime()",
      "org.eclipse.jgit.revwalk.RevCommit.getFullMessage()",
      "org.eclipse.jgit.revwalk.RevCommit.getId()",
      "org.eclipse.jgit.revwalk.RevCommit.getTree()",
      "org.eclipse.jgit.revwalk.RevTree.getId()",
  };

  private static boolean isUIThreadUnsafe(AccessTarget.CodeUnitCallTarget codeUnit) {
    String packageName = codeUnit.getOwner().getPackageName();
    String fullName = codeUnit.getFullName();

    return uiThreadUnsafePackagePrefixes.stream().anyMatch(prefix -> packageName.startsWith(prefix))
        && !uiThreadSafeCodeUnitsInUnsafePackages.asList().contains(fullName)
        || knownBlockingCodeUnits.asList().contains(fullName)
        || codeUnit.isAnnotatedWith(UIThreadUnsafe.class);
  }

  private static final String[] knownMethodsOverridableAsUIThreadUnsafe = {
      // These two methods have been experimentally verified to be executed by IntelliJ outside of UI thread.
      "com.intellij.codeInsight.completion.CompletionContributor.fillCompletionVariants(com.intellij.codeInsight.completion.CompletionParameters, com.intellij.codeInsight.completion.CompletionResultSet)",
      "com.intellij.lang.annotation.Annotator.annotate(com.intellij.psi.PsiElement, com.intellij.lang.annotation.AnnotationHolder)",
      // This method (overridden in Backgroundables) is meant to run outside of UI thread by design.
      "com.intellij.openapi.progress.Progressive.run(com.intellij.openapi.progress.ProgressIndicator)",
  };

  @Test
  public void ui_thread_unsafe_methods_should_not_override_ui_thread_safe_methods() {
    noMethods()
        .that()
        .areAnnotatedWith(UIThreadUnsafe.class)
        .should(overrideAnyMethodThat("is NOT ${UIThreadUnsafeName} itself", (method, overriddenMethod) -> {
          val overriddenMethodFullName = overriddenMethod.getFullName();
          if (overriddenMethod.isAnnotatedWith(UIThreadUnsafe.class)) {
            return false;
          }
          if (knownMethodsOverridableAsUIThreadUnsafe.asList().contains(overriddenMethodFullName)) {
            return false;
          }
          return true;
        }))
        .check(productionClasses);
  }

  @Test
  public void concrete_ui_thread_unsafe_code_units_should_call_at_least_one_ui_thread_unsafe_code_unit() {
    codeUnits()
        .that()
        .doNotHaveModifier(ABSTRACT)
        .and()
        .areAnnotatedWith(UIThreadUnsafe.class)
        .should(callAtLeastOnceACodeUnitThat("is UI-thread unsafe",
            (codeUnit, calledCodeUnit) -> isUIThreadUnsafe(calledCodeUnit)
                && !extractWhitelistedCodeUnitsFromAnnotation(codeUnit).contains(calledCodeUnit.getFullName())))
        .check(productionClasses);
  }
}
