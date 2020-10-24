package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import org.junit.Test;

public class InheritanceTestSuite extends BaseArchUnitTestSuite {

  @Test
  public void actions_implementing_DumbAware_should_extend_DumbAwareAction() {
    classes()
        .that().areAssignableTo(com.intellij.openapi.actionSystem.AnAction.class)
        .and().implement(com.intellij.openapi.project.DumbAware.class)
        .should().beAssignableTo(com.intellij.openapi.project.DumbAwareAction.class)
        .because("`extends DumbAwareAction` should be used instead of " +
            "extending `AnAction` and implementing `DumbAware` separately")
        .check(importedClasses);
  }

}
