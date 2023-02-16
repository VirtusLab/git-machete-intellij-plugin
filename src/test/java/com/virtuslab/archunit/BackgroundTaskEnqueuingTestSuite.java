package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.openapi.progress.Task;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import git4idea.branch.GitBrancher;
import org.junit.jupiter.api.Test;

import com.virtuslab.qual.async.BackgroundableQueuedElsewhere;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.async.DoesNotContinueInBackground;

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
  public void only_continues_in_background_methods_should_call_other_continues_in_background_methods() {
    noMethods()
        .that()
        .areNotAnnotatedWith(ContinuesInBackground.class)
        .and()
        .areNotAnnotatedWith(DoesNotContinueInBackground.class)
        .should(callAnyCodeUnitsThat("are annotated with ${ContinuesInBackgroundName}",
            (codeUnit, calledCodeUnit) -> calledCodeUnit.isAnnotatedWith(ContinuesInBackground.class)))
        .check(productionClasses);
  }

  @Test
  public void only_continues_in_background_methods_should_enqueue_background_tasks() {
    noMethods()
        .that()
        .areNotAnnotatedWith(ContinuesInBackground.class)
        .and()
        .areNotAnnotatedWith(DoesNotContinueInBackground.class)
        .should(callAnyCodeUnitsThat("enqueue background tasks", (codeUnit, calledCodeUnit) -> {
          String calledCodeUnitName = calledCodeUnit.getName();
          JavaClass calledCodeUnitOwner = calledCodeUnit.getOwner();
          return calledCodeUnitOwner.isAssignableTo(Task.Backgroundable.class) && calledCodeUnitName.equals("queue") ||
              calledCodeUnitOwner.isAssignableTo(GitBrancher.class)
                  && !(calledCodeUnitName.equals("getInstance") || calledCodeUnitName.equals("compareAny"))
              ||
              calledCodeUnitOwner.isAssignableTo(VcsPushDialog.class) && calledCodeUnitName.equals("show") ||
              calledCodeUnit.getRawReturnType().isEquivalentTo(java.util.concurrent.Future.class);
        }))
        .because("running tasks in background is inextricably linked to the increased risk of race conditions; " +
            "mark the calling method as ${ContinuesInBackgroundName} to make it clear that it executes asynchronously")
        .check(productionClasses);
  }
}
