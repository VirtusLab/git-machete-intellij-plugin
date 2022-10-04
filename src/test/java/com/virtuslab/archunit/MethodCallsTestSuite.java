package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.elements.GivenCodeUnits;
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
