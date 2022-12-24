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
  public void actions_overriding_onUpdate_should_call_super_onUpdate() {
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
    }).check(importedClasses);
  }

  @Test
  public void onUpdate_methods_calling_Presentation_setEnabled_must_earlier_call_Presentation_isEnabled_too() {
    String isEnabledMethodFullName = "com.intellij.openapi.actionSystem.Presentation.isEnabled()";
    String isEnabledAndVisibleMethodFullName = "com.intellij.openapi.actionSystem.Presentation.isEnabledAndVisible()";
    String setEnabledMethodFullName = "com.intellij.openapi.actionSystem.Presentation.setEnabled(boolean)";

    methods().that().haveName("onUpdate")
        .and().areNotDeclaredIn(com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction.class)
        .should(new ArchCondition<>("call Presentation.isEnabled or Presentation.isEnabledAndVisible first " +
            "if they call Presentation.setEnabled") {

          @Override
          public void check(JavaMethod method, ConditionEvents events) {
            JavaMethodCall setEnabledMethodCall = findFirstCallTo(method, setEnabledMethodFullName);
            JavaMethodCall isEnabledMethodCall = findFirstCallTo(method, isEnabledMethodFullName);
            JavaMethodCall isEnabledAndVisibleMethodCall = findFirstCallTo(method, isEnabledAndVisibleMethodFullName);

            val messagePrefix = "Method ${method.getFullName()} calls Presentation::setEnabled ";
            val messageSuffix = "; this might lead to accidentally re-enabling an action that has already been disabled by super.onUpdate()";
            if (setEnabledMethodCall != null) {
              if (isEnabledMethodCall == null && isEnabledAndVisibleMethodCall == null) {
                String message = messagePrefix
                    + "without a preceding Presentation::isEnabled or Presentation::isEnabledAndVisible" + messageSuffix;
                events.add(SimpleConditionEvent.violated(method, message));
              } else {
                if (isEnabledMethodCall != null && setEnabledMethodCall.getLineNumber() < isEnabledMethodCall.getLineNumber()) {
                  String message = messagePrefix + "before Presentation::isEnabled" + messageSuffix;
                  events.add(SimpleConditionEvent.violated(method, message));
                }
                if (isEnabledAndVisibleMethodCall != null
                    && setEnabledMethodCall.getLineNumber() < isEnabledAndVisibleMethodCall.getLineNumber()) {
                  String message = messagePrefix + "before Presentation::isEnabledAndVisible" + messageSuffix;
                  events.add(SimpleConditionEvent.violated(method, message));
                }
              }
            }
          }
        }).check(importedClasses);
  }

  @Test
  public void onUpdate_methods_calling_Presentation_setVisible_must_earlier_call_Presentation_isVisible_too() {
    String isVisibleMethodFullName = "com.intellij.openapi.actionSystem.Presentation.isVisible()";
    String isEnabledAndVisibleMethodFullName = "com.intellij.openapi.actionSystem.Presentation.isEnabledAndVisible()";
    String setVisibleMethodFullName = "com.intellij.openapi.actionSystem.Presentation.setVisible(boolean)";

    methods().that().haveName("onUpdate")
        .should(new ArchCondition<>("call Presentation.isVisible or Presentation.isEnabledAndVisible first " +
            "if they call Presentation.setVisible") {

          @Override
          public void check(JavaMethod method, ConditionEvents events) {
            JavaMethodCall setVisibleMethodCall = findFirstCallTo(method, setVisibleMethodFullName);
            JavaMethodCall isVisibleMethodCall = findFirstCallTo(method, isVisibleMethodFullName);
            JavaMethodCall isEnabledAndVisibleMethodCall = findFirstCallTo(method, isEnabledAndVisibleMethodFullName);

            val messagePrefix = "Method ${method.getFullName()} calls Presentation::setVisible ";
            val messageSuffix = "; this might lead to accidentally un-hiding an action that has already been hidden by super.onUpdate()";
            if (setVisibleMethodCall != null) {
              if (isVisibleMethodCall == null && isEnabledAndVisibleMethodCall == null) {
                String message = messagePrefix
                    + "without a preceding Presentation::isVisible or Presentation::isEnabledAndVisible" + messageSuffix;
                events.add(SimpleConditionEvent.violated(method, message));
              } else {
                if (isVisibleMethodCall != null && setVisibleMethodCall.getLineNumber() < isVisibleMethodCall.getLineNumber()) {
                  String message = messagePrefix + "before Presentation::isVisible" + messageSuffix;
                  events.add(SimpleConditionEvent.violated(method, message));
                }
                if (isEnabledAndVisibleMethodCall != null
                    && setVisibleMethodCall.getLineNumber() < isEnabledAndVisibleMethodCall.getLineNumber()) {
                  String message = messagePrefix + "before Presentation::isEnabledAndVisible" + messageSuffix;
                  events.add(SimpleConditionEvent.violated(method, message));
                }
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
