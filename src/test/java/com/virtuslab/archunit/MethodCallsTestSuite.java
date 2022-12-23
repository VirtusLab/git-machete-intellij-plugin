package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Test;

public class MethodCallsTestSuite extends BaseArchUnitTestSuite {

  @Test
  public void actions_overriding_onUpdate_should_call_super_onUpdate() {
    classes()
        .that()
        .areAssignableTo(com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction.class)
        .and()
        .areNotAssignableFrom(com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction.class)
        .and(new DescribedPredicate<JavaClass>("override onUpdate method") {
          @Override
          public boolean test(JavaClass input) {
            return input.getMethods().stream().anyMatch(method -> method.getName().equals("onUpdate"));
          }
        })
        .should()
        .callMethodWhere(
            new DescribedPredicate<JavaMethodCall>("name is onUpdate and owner is the direct superclass") {
              @Override
              public boolean test(JavaMethodCall input) {
                JavaCodeUnit origin = input.getOrigin(); // where is the method called from?
                AccessTarget.MethodCallTarget target = input.getTarget(); // where is the method declared?

                if (origin.getName().equals("onUpdate") && target.getName().equals("onUpdate")) {
                  return target.getOwner().equals(origin.getOwner().getSuperclass().orElse(null));
                }
                return false;
              }
            })
        .check(importedClasses);

  }

  @Test
  public void methods_calling_MessageBusConnection_subscribe_must_later_call_Disposer_register_too() {
    String subscribeMethodFullName = "com.intellij.util.messages.MessageBusConnection.subscribe(com.intellij.util.messages.Topic, java.lang.Object)";
    String registerMethodFullName = "com.intellij.openapi.util.Disposer.register(com.intellij.openapi.Disposable, com.intellij.openapi.Disposable)";

    codeUnits().should(new ArchCondition<>("call ${registerMethodFullName} if they call ${subscribeMethodFullName}") {

      @Override
      public void check(JavaCodeUnit codeUnit, ConditionEvents events) {
        JavaMethodCall subscribeCall = findFirstCallTo(codeUnit, subscribeMethodFullName);
        JavaMethodCall registerCall = findFirstCallTo(codeUnit, registerMethodFullName);

        if (subscribeCall != null) {
          if (registerCall == null) {
            String message = "Method ${codeUnit.getFullName()} calls MessageBusConnection::subscribe without a following Disposer::register";
            events.add(SimpleConditionEvent.violated(codeUnit, message));
          } else if (registerCall.getLineNumber() < subscribeCall.getLineNumber()) {
            String message = "Method ${codeUnit.getFullName()} calls MessageBusConnection::subscribe after Disposer::register";
            events.add(SimpleConditionEvent.violated(codeUnit, message));
          }
        }
      }
    }).check(importedClasses);
  }

  private static @Nullable JavaMethodCall findFirstCallTo(JavaCodeUnit sourceCodeUnit, String targetMethodFullName) {
    return sourceCodeUnit.getMethodCallsFromSelf().stream()
        .filter(call -> call.getTarget().getFullName().equals(targetMethodFullName)).findFirst()
        .orElse(null);
  }

}
