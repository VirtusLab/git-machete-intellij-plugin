package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.*;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Test;

public class MethodCallsTestSuite extends BaseArchUnitTestSuite {

  @Test
  public void overridden_onUpdate_methods_should_call_super_onUpdate() {
    methods().that().haveName("onUpdate")
        .and().areNotDeclaredIn(com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction.class)
        .should(new ArchCondition<>("call onUpdate from the direct superclass") {

          @Override
          public void check(JavaMethod method, ConditionEvents events) {
            val superclass = method.getOwner().getSuperclass().orElse(null);
            if (method.getCallsFromSelf().stream().noneMatch(call -> isACallTo(call, superclass, "onUpdate"))) {
              String message = "Method ${method.getFullName()} does not call super.onUpdate()";
              events.add(SimpleConditionEvent.violated(method, message));
            }
          }

          private boolean isACallTo(JavaCall<?> call, JavaType methodOwner, String methodName) {
            return call.getTarget().getOwner().equals(methodOwner) && call.getTarget().getName().equals(methodName);
          }
        })
        .check(importedClasses);

  }

  @Test
  public void code_units_calling_MessageBusConnection_subscribe_must_later_call_Disposer_register_too() {
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

      private @Nullable JavaMethodCall findFirstCallTo(JavaCodeUnit sourceCodeUnit, String targetMethodFullName) {
        return sourceCodeUnit.getMethodCallsFromSelf().stream()
            .filter(call -> call.getTarget().getFullName().equals(targetMethodFullName)).findFirst()
            .orElse(null);
      }
    }).check(importedClasses);
  }
}
