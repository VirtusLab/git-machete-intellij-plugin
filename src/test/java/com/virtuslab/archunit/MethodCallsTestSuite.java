package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.*;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;

public class MethodCallsTestSuite extends BaseArchUnitTestSuite {

  @Test
  public void methods_calling_a_method_throwing_RevisionSyntaxException_should_catch_it_explicitly() {
    codeUnits().that(new DescribedPredicate<JavaCodeUnit>("call a code unit throwing JGit RevisionSyntaxException") {

      @Override
      public boolean test(JavaCodeUnit codeUnit) {
        return codeUnit.getCallsFromSelf().stream().anyMatch(
            call -> call.getTarget().getThrowsClause().containsType(org.eclipse.jgit.errors.RevisionSyntaxException.class));
      }
    })
        .should(new ArchCondition<JavaCodeUnit>("catch this exception explicitly") {
          @Override
          public void check(JavaCodeUnit codeUnit, ConditionEvents events) {
            if (codeUnit.getTryCatchBlocks().stream().noneMatch(block -> block.getCaughtThrowables().stream()
                .anyMatch(javaClass -> javaClass.isEquivalentTo(org.eclipse.jgit.errors.RevisionSyntaxException.class)))) {
              events.add(SimpleConditionEvent.violated(codeUnit,
                  "code unit ${codeUnit.getFullName()} does not catch RevisionSyntaxException; see issue #1826"));
            }
          }
        })
        .check(productionClasses);
  }

  @Test
  public void overridden_onUpdate_methods_should_call_super_onUpdate() {
    methods().that().haveName("onUpdate")
        .and().areNotDeclaredIn(com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction.class)
        .should(callAtLeastOnceACodeUnitThat("is called onUpdate and is declared in the direct superclass",
            (method, calledMethod) -> {
              val superclass = method.getOwner().getSuperclass().orElse(null);
              return calledMethod.getOwner().equals(superclass) && calledMethod.getName().equals("onUpdate");
            }))
        .check(productionClasses);
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
    }).check(productionClasses);
  }
}
