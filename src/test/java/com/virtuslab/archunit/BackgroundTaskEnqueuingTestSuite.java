package com.virtuslab.archunit;

import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import java.util.Arrays;

import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.openapi.progress.Task;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import git4idea.branch.GitBrancher;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.junit.jupiter.api.Test;

import com.virtuslab.qual.async.BackgroundableQueuedElsewhere;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.async.DoesNotContinueInBackground;

@ExtensionMethod(Arrays.class)
public class BackgroundTaskEnqueuingTestSuite extends BaseArchUnitTestSuite {

  private static final String ContinuesInBackgroundName = "@" + ContinuesInBackground.class.getSimpleName();

  @Test
  public void methods_that_create_backgroundable_should_also_call_queue() {
    methods()
        .that()
        .areNotAnnotatedWith(BackgroundableQueuedElsewhere.class)
        .and(new DescribedPredicate<JavaMethod>("create an instance of Task.Backgroundable or its subclass") {
          @Override
          public boolean test(JavaMethod method) {
            return method.getConstructorCallsFromSelf().stream()
                .anyMatch(constructorCall -> constructorCall.getTargetOwner().isAssignableTo(Task.Backgroundable.class));
          }
        })
        .should(callAtLeastOnceACodeUnitThat("is Task.Backgroundable#queue()",
            (codeUnit, calledCodeUnit) -> calledCodeUnit.getOwner().isAssignableTo(Task.Backgroundable.class)
                && calledCodeUnit.getName().equals("queue")))
        .because("otherwise it's likely that you forgot about actually scheduling the task; " +
            "mark the method with ${BackgroundableQueuedElsewhere.class.getSimpleName()} if this is expected")
        .check(productionClasses);
  }

  @Test
  public void only_continues_in_background_methods_should_call_methods_that_continue_in_background() {
    noMethods()
        .that()
        .areNotAnnotatedWith(ContinuesInBackground.class)
        .and()
        .areNotAnnotatedWith(DoesNotContinueInBackground.class)
        .should(callAnyCodeUnitsThat("enqueue background tasks or are themselves ${ContinuesInBackgroundName}",
            (codeUnit, calledCodeUnit) -> doesContinueInBackground(calledCodeUnit)))
        .because("running tasks in background is inextricably linked to the increased risk of race conditions; " +
            "mark the calling method as ${ContinuesInBackgroundName} to make it clear that it executes asynchronously")
        .check(productionClasses);
  }

  private static final String[] knownMethodsOverridableAsContinuesInBackground = {
      "com.intellij.codeInsight.intention.IntentionAction.invoke(com.intellij.openapi.project.Project, com.intellij.openapi.editor.Editor, com.intellij.psi.PsiFile)",
      "com.intellij.openapi.actionSystem.AnAction.actionPerformed(com.intellij.openapi.actionSystem.AnActionEvent)",
      "com.intellij.openapi.progress.Task.onFinished()",
      "com.intellij.openapi.progress.Task.onSuccess()",
      "com.intellij.openapi.vfs.newvfs.BulkFileListener.after(java.util.List)",
      "com.intellij.ui.AncestorListenerAdapter.ancestorAdded(javax.swing.event.AncestorEvent)",
      com.virtuslab.gitmachete.frontend.actions.backgroundables.SideEffectingBackgroundable.class.getName()
          + ".doRun(com.intellij.openapi.progress.ProgressIndicator)",
      com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction.class.getName()
          + ".actionPerformed(com.intellij.openapi.actionSystem.AnActionEvent)",
      "javax.swing.event.AncestorListener.ancestorAdded(javax.swing.event.AncestorEvent)",
  };

  @Test
  public void continues_in_background_methods_should_not_override_non_continues_in_background_methods() {
    noMethods()
        .that()
        .areAnnotatedWith(ContinuesInBackground.class)
        .should(overrideAnyMethodThat("is NOT ${ContinuesInBackgroundName} itself", (method, overriddenMethod) -> {
          val overriddenMethodFullName = overriddenMethod.getFullName();
          if (overriddenMethod.isAnnotatedWith(ContinuesInBackground.class)) {
            return false;
          }
          if (knownMethodsOverridableAsContinuesInBackground.asList().contains(overriddenMethodFullName)) {
            return false;
          }
          return true;
        }))
        .check(productionClasses);
  }

  @Test
  public void concrete_continues_in_background_methods_should_call_at_least_one_method_that_continues_in_background() {
    methods()
        .that()
        .doNotHaveModifier(ABSTRACT)
        .and()
        .areAnnotatedWith(ContinuesInBackground.class)
        .should(callAtLeastOnceACodeUnitThat("enqueue background tasks or are themselves ${ContinuesInBackgroundName}",
            (codeUnit, calledCodeUnit) -> doesContinueInBackground(calledCodeUnit)))
        .check(productionClasses);
  }

  private static boolean doesContinueInBackground(AccessTarget.CodeUnitCallTarget codeUnit) {
    if (codeUnit.isAnnotatedWith(ContinuesInBackground.class)) {
      return true;
    }

    String name = codeUnit.getName();
    JavaClass owner = codeUnit.getOwner();
    JavaClass returnType = codeUnit.getRawReturnType();

    return owner.isAssignableTo(Task.Backgroundable.class) && name.equals("queue") ||
        owner.isAssignableTo(GitBrancher.class) && !(name.equals("getInstance") || name.equals("compareAny")) ||
        owner.isAssignableTo(VcsPushDialog.class) && name.equals("show") ||
        returnType.isEquivalentTo(java.util.concurrent.Future.class);
  }
}
