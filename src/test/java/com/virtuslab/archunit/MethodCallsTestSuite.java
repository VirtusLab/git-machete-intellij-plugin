package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.elements.GivenCodeUnits;
import org.junit.Test;

public class MethodCallsTestSuite extends BaseArchUnitTestSuite {

  @Test
  public void methods_calling_MessageBusConnection_subscribe_must_call_Disposer_register_later_too() {
    String subscribeMethodCallString = "com.intellij.util.messages.MessageBusConnection.subscribe(com.intellij.util.messages.Topic, java.lang.Object)";
    String registerMethodCallString = "com.intellij.openapi.util.Disposer.register(com.intellij.openapi.Disposable, com.intellij.openapi.Disposable)";

    List<GivenCodeUnits<? extends JavaCodeUnit>> codeUnits = Arrays.asList(constructors(), methods());
    for (GivenCodeUnits<? extends JavaCodeUnit> c : codeUnits) {

      c.should(new ArchCondition<JavaCodeUnit>(
          "call ${registerMethodCallString} if they call ${subscribeMethodCallString}") {

        @Override
        public void check(JavaCodeUnit item, ConditionEvents events) {
          Optional<JavaMethodCall> subscribeCall = item.getMethodCallsFromSelf().stream()
              .filter(innerMethod -> innerMethod.getTarget().getFullName().equals(subscribeMethodCallString)).findAny();

          Optional<JavaMethodCall> registerCall = item.getMethodCallsFromSelf().stream()
              .filter(innerMethod -> innerMethod.getTarget().getFullName().equals(registerMethodCallString)).findAny();

          if (subscribeCall.isPresent()) {
            int subscribeCallLine = subscribeCall.map(methodCall -> methodCall.getLineNumber()).get();
            if (registerCall.isEmpty()) {
              String message = "Method ${item.getFullName()} calls MessageBusConnection::subscribe without a following Disposer::register";
              events.add(SimpleConditionEvent.violated(item, message));
            } else if (registerCall.map(x -> x.getLineNumber() < subscribeCallLine).orElse(false)) {
              String message = "Method ${item.getFullName()} calls MessageBusConnection::subscribe after Disposer::register";
              events.add(SimpleConditionEvent.violated(item, message));
            }
          }
        }
      }).check(importedClasses);

    }
  }

}
