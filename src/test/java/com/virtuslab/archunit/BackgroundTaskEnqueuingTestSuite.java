package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import java.util.Arrays;

import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.openapi.progress.Task;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import git4idea.branch.GitBrancher;
import io.vavr.collection.List;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.junit.jupiter.api.Test;

import com.virtuslab.qual.async.BackgroundableQueuedElsewhere;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.async.DoesNotContinueInBackground;
import com.virtuslab.qual.guieffect.IgnoreUIThreadUnsafeCalls;

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
        .should(new ArchCondition<JavaMethod>("call Task.Backgroundable#queue()") {
          @Override
          public void check(JavaMethod method, ConditionEvents events) {
            if (method.getMethodCallsFromSelf().stream().noneMatch(this::isTaskBackgroundableQueue)) {
              events.add(SimpleConditionEvent.violated(method, "${method} doesn't call Task.Backgroundable#queue()"));
            }
          }

          private boolean isTaskBackgroundableQueue(JavaMethodCall call) {
            val callTarget = call.getTarget();
            return callTarget.getOwner().isAssignableTo(Task.Backgroundable.class)
                && callTarget.getName().equals("queue");
          }
        })
        .because("otherwise it's likely that you forgot about actually scheduling the task; " +
            "mark the method with ${BackgroundableQueuedElsewhere.class.getSimpleName()} if this is expected")
        .check(productionClasses);
  }

  private static List<String> extractWhitelistedMethodsFromAnnotation(JavaMethod method) {
    if (method.isAnnotatedWith(IgnoreUIThreadUnsafeCalls.class)) {
      return List.of(method.getAnnotationOfType(IgnoreUIThreadUnsafeCalls.class).value());
    } else {
      return List.empty();
    }
  }

  @Test
  public void only_continues_in_background_methods_should_call_other_continues_in_background_methods() {
    methods()
        .that()
        .areNotAnnotatedWith(ContinuesInBackground.class)
        .and()
        .areNotAnnotatedWith(DoesNotContinueInBackground.class)
        .should(new ArchCondition<JavaMethod>("never call any ${ContinuesInBackgroundName} methods") {
          @Override
          public void check(JavaMethod method, ConditionEvents events) {
            val whitelistedMethodsFromAnnotation = extractWhitelistedMethodsFromAnnotation(method);
            method.getCallsFromSelf().forEach(call -> {
              AccessTarget calledMethod = call.getTarget();
              String calledMethodFullName = calledMethod.getFullName();
              if (calledMethod.isAnnotatedWith(ContinuesInBackground.class)
                  && !whitelistedMethodsFromAnnotation.contains(calledMethodFullName)) {
                String message = "a non-${ContinuesInBackgroundName} method ${method.getFullName()} " +
                    "calls a ${ContinuesInBackgroundName} method ${calledMethodFullName}";
                events.add(SimpleConditionEvent.violated(method, message));
              }
            });
          }
        })
        .check(productionClasses);
  }

  @Test
  public void only_continues_in_background_methods_should_enqueue_background_tasks() {
    methods()
        .that()
        .areNotAnnotatedWith(ContinuesInBackground.class)
        .and()
        .areNotAnnotatedWith(DoesNotContinueInBackground.class)
        .should(new ArchCondition<JavaMethod>("never enqueue background tasks") {
          @Override
          public void check(JavaMethod method, ConditionEvents events) {
            method.getCallsFromSelf().forEach(call -> {
              val callTarget = call.getTarget();
              String callTargetName = callTarget.getName();
              JavaClass callTargetOwner = callTarget.getOwner();
              if (callTargetOwner.isAssignableTo(Task.Backgroundable.class) && callTargetName.equals("queue") ||
                  callTargetOwner.isAssignableTo(GitBrancher.class) && !callTargetName.equals("getInstance") ||
                  callTargetOwner.isAssignableTo(VcsPushDialog.class) && callTargetName.equals("show") ||
                  callTarget.getRawReturnType().isEquivalentTo(java.util.concurrent.Future.class)) {
                String message = "a non-${ContinuesInBackgroundName} method ${method.getFullName()} " +
                    "enqueues a background task via method ${callTarget.getFullName()}; " +
                    "mark this method as ${ContinuesInBackgroundName} if you're aware of the race conditions " +
                    "inherent in running tasks in background";
                events.add(SimpleConditionEvent.violated(method, message));
              }
            });
          }
        })
        .check(productionClasses);
  }
}
