package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import java.util.Arrays;

import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaClass;
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
            if (method.getName().startsWith("access$") || method.getName().startsWith("lambda$")) {
              return;
            }

            // AspectJ puts the original method body within an `<original-method-name>_aroundBodyX` synthetic method,
            // while the original method body is replaced with AspectJ-generated code
            // that includes the woven-in action (e.g. logging, in case of @Loggable)
            // and calls the actual logic in `..._aroundBodyX`.
            if (method.getName().matches("^.*_aroundBody[0-9]$")) {
              // An `..._aroundBodyX` method does NOT inherit the annotations of the original method,
              // so it won't be filtered out by `.areNotAnnotatedWith(UIThreadUnsafe.class)` condition.
              // This might cause a false positive: if the original method is annotated with @UIThreadUnsafe
              // AND its body has a call to an @UIThreadUnsafe-annotated method,
              // then ArchUnit will incorrectly report an error.

              // To prevent such cases, let's get the original method...
              Class<?>[] parameterClasses = method.getRawParameterTypes().stream()
                  .map(JavaClass::reflect)
                  // Let's skip `this` param
                  .skip(1)
                  // ...and the final param, both added by AspectJ to what's in the original method
                  .filter(clazz -> !clazz.getName().equals("org.aspectj.lang.JoinPoint"))
                  .toArray(Class[]::new);

              JavaMethod originalMethod = method.getOwner()
                  .tryGetMethod(method.getName().replaceAll("_aroundBody[0-9]$", ""), parameterClasses).orElse(null);

              // ...and check if the original method qualifies under `.areNotAnnotatedWith(UIThreadUnsafe.class)` condition.
              if (originalMethod != null && originalMethod.isAnnotatedWith(UIThreadUnsafe.class)) {
                return;
              }
            }

            method.getCallsFromSelf().forEach(access -> {
              AccessTarget accessTarget = access.getTarget();
              if (accessTarget.isAnnotatedWith(UIThreadUnsafe.class)) {
                String message = "a non-${UIThreadUnsafeName} method ${method.getFullName()} " +
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
            if (method.getName().startsWith("access$") || method.getName().startsWith("lambda$")) {
              return;
            }

            method.getCallsFromSelf().forEach(call -> {
              AccessTarget.CodeUnitCallTarget callTarget = call.getTarget();
              String callTargetPackageName = callTarget.getOwner().getPackageName();
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
                    "git4idea.repo.GitRepository.getProject()",
                    "git4idea.repo.GitRepository.getRemotes()",
                    "git4idea.repo.GitRepository.getRoot()",
                    "git4idea.repo.GitRepository.getState()",
                    "git4idea.repo.GitRepository.getVcs()",
                    "git4idea.validators.GitBranchValidatorKt.checkRefName(java.lang.String)"
                };
                String calledMethodFullName = callTarget.getFullName();

                if (!Arrays.asList(whitelistedMethodFullNames).contains(calledMethodFullName)) {
                  String message = "a non-${UIThreadUnsafeName} method ${method.getFullName()} " +
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
