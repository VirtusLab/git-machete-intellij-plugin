package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import java.util.Arrays;

import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.vavr.collection.List;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.junit.Test;

import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.async.DoesNotContinueInBackground;
import com.virtuslab.qual.guieffect.IgnoreUIThreadUnsafeCalls;

@ExtensionMethod(Arrays.class)
public class BackgroundTaskEnqueuingTestSuite extends BaseArchUnitTestSuite {

  private static final String ContinuesInBackgroundName = "@" + ContinuesInBackground.class.getSimpleName();

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
            // This makes the check somewhat unsound (some non-UI-safe calls can slip under the radar in lambdas),
            // but annotating lambda methods isn't possible without expanding lambda into anonymous class...
            // which would in turn heavily reduce readability, hence potentially leading to bugs in the long run.
            if (method.getName().equals("$deserializeLambda$")) {
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

              // ...and check if the original method qualifies under `.areNotAnnotatedWith(ContinuesInBackground.class)` condition.
              if (originalMethod != null && originalMethod.isAnnotatedWith(ContinuesInBackground.class)) {
                return;
              }
            }

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
        .check(importedClasses);
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
              if (callTargetName.equals("queue")
                  && callTargetOwner.isAssignableTo(com.intellij.openapi.progress.Task.Backgroundable.class) ||
                  callTargetName.equals("checkout") && callTargetOwner.isAssignableTo(git4idea.branch.GitBrancher.class) ||
                  callTargetName.equals("renameBranch") && callTargetOwner.isAssignableTo(git4idea.branch.GitBrancher.class) ||
                  callTargetName.equals("show")
                      && callTargetOwner.isAssignableTo(com.intellij.dvcs.push.ui.VcsPushDialog.class)) {
                String message = "a non-${ContinuesInBackgroundName} method ${method.getFullName()} " +
                    "enqueues a background task via method ${callTarget.getFullName()}; " +
                    "mark this method as ${ContinuesInBackgroundName} if you're aware of the race conditions " +
                    "inherent in running tasks in background";
                events.add(SimpleConditionEvent.violated(method, message));
              }
            });
          }
        })
        .check(importedClasses);
  }
}
