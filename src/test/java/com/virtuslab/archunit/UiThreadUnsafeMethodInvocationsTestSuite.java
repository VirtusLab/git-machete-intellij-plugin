package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import java.util.Arrays;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.junit.Test;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public class UiThreadUnsafeMethodInvocationsTestSuite extends BaseArchUnitTestSuite {

  private static final String UIThreadUnsafeName = "@" + UIThreadUnsafe.class.getSimpleName();

  @Test
  public void ui_thread_unsafe_methods_should_not_be_uieffect() {
    methods()
        .that()
        .areAnnotatedWith(UIThreadUnsafe.class)
        .should()
        .notBeAnnotatedWith(UIEffect.class)
        .check(importedClasses);
  }

  @Test
  public void only_ui_thread_unsafe_methods_should_call_other_ui_thread_unsafe_methods() {
    methods()
        .that()
        .areNotAnnotatedWith(UIThreadUnsafe.class)
        .should(new ArchCondition<JavaMethod>("never call any ${UIThreadUnsafeName} methods") {
          @Override
          public void check(JavaMethod method, ConditionEvents events) {
            // This makes the check somewhat unsound (some non-UI-safe calls can slip under the radar in lambdas),
            // but annotating lambda methods isn't possible without expanding lambda into anonymous class...
            // which would in turn heavily reduce readability, hence potentially leading to bugs in the long run.
            if (method.getName().startsWith("lambda$")) {
              return;
            }

            method.getCallsFromSelf().forEach(access -> {
              var accessTarget = access.getTarget();
              if (accessTarget.isAnnotatedWith(UIThreadUnsafe.class)) {
                var message = "a non-${UIThreadUnsafeName} method ${method.getFullName()} " +
                    "calls a ${UIThreadUnsafeName} method ${accessTarget.getFullName()}";
                events.add(SimpleConditionEvent.violated(method, message));
              }
            });
          }
        })
        .check(importedClasses);
  }

  @Test
  public void only_ui_thread_unsafe_method_should_call_git4idea_methods() {
    methods()
        .that()
        .areNotAnnotatedWith(UIThreadUnsafe.class)
        .should(new ArchCondition<JavaMethod>("never call any heavyweight git4idea methods") {
          @Override
          public void check(JavaMethod method, ConditionEvents events) {
            if (method.getName().startsWith("lambda$")) {
              return;
            }

            method.getCallsFromSelf().forEach(call -> {
              var callTarget = call.getTarget();
              var callTargetPackageName = callTarget.getOwner().getPackageName();
              if (callTargetPackageName.startsWith("git4idea")) {
                String[] whitelistedMethodFullNames = {
                    "git4idea.GitLocalBranch.getName()",
                    "git4idea.GitRemoteBranch.getName()",
                    "git4idea.GitUtil.findGitDir(com.intellij.openapi.vfs.VirtualFile)",
                    "git4idea.GitUtil.getRepositories(com.intellij.openapi.project.Project)",
                    "git4idea.GitUtil.getRepositoryManager(com.intellij.openapi.project.Project)",
                    "git4idea.branch.GitBranchesCollection.findLocalBranch(java.lang.String)",
                    "git4idea.branch.GitBranchesCollection.findRemoteBranch(java.lang.String)",
                    "git4idea.branch.GitNewBranchDialog.<init>(com.intellij.openapi.project.Project, java.util.Collection, java.lang.String, java.lang.String, boolean, boolean, boolean)",
                    "git4idea.branch.GitNewBranchDialog.showAndGetOptions()",
                    "git4idea.branch.GitNewBranchOptions.getName()",
                    "git4idea.branch.GitNewBranchOptions.shouldCheckout()",
                    "git4idea.config.GitSharedSettings.getInstance(com.intellij.openapi.project.Project)",
                    "git4idea.config.GitSharedSettings.isBranchProtected(java.lang.String)",
                    "git4idea.config.GitVcsSettings.getInstance(com.intellij.openapi.project.Project)",
                    "git4idea.config.GitVcsSettings.getRecentRootPath()",
                    "git4idea.fetch.GitFetchSupport.isFetchRunning()",
                    "git4idea.fetch.GitFetchResult.showNotification()",
                    "git4idea.fetch.GitFetchSupport.fetchSupport(com.intellij.openapi.project.Project)",
                    "git4idea.push.GitPushSource.create(git4idea.GitLocalBranch)",
                    "git4idea.repo.GitRemote.getName()",
                    "git4idea.repo.GitRepository.getBranches()",
                    "git4idea.repo.GitRepository.getCurrentBranch()",
                    "git4idea.repo.GitRepository.getRemotes()",
                    "git4idea.repo.GitRepository.getRoot()",
                    "git4idea.repo.GitRepository.getState()",
                    "git4idea.repo.GitRepository.getVcs()",
                    "git4idea.validators.GitBranchValidatorKt.checkRefName(java.lang.String)"
                };
                var calledMethodFullName = callTarget.getFullName();

                if (!Arrays.asList(whitelistedMethodFullNames).contains(calledMethodFullName)) {
                  var message = "a non-${UIThreadUnsafeName} method ${method.getFullName()} " +
                      "calls method ${calledMethodFullName} from git4idea";
                  events.add(SimpleConditionEvent.violated(method, message));
                }
              }
            });
          }
        })
        .check(importedClasses);
  }
}
