package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.Test;

public class NamingTestSuite extends BaseArchUnitTestSuite {

  @Test
  public void exception_class_names_should_end_with_Exception() {
    classes()
        .that().areAssignableTo(Exception.class)
        .should().haveSimpleNameEndingWith("Exception")
        .check(productionClasses);
  }

  @Test
  public void interface_names_should_start_with_I() {
    // Checking for @Target seems the best available way to identify annotations
    classes()
        .that().areInterfaces()
        .and().resideOutsideOfPackage("com.virtuslab.gitmachete.frontend.file.grammar")
        .and().areNotAnnotatedWith(java.lang.annotation.Target.class)
        .should().haveSimpleNameStartingWith("I")
        .check(productionClasses);
  }
  @Test
  public void class_names_should_not_end_with_Manager() {
    noClasses()
        .should().haveSimpleNameEndingWith("Manager")
        .because("classes called `...Manager` are an indicator of poor design; " +
            "likely a redesign (and not just a rename) is needed")
        .check(productionClasses);
  }

  @Test
  public void class_names_should_not_end_with_Util() {
    noClasses()
        .should().haveSimpleNameEndingWith("Util")
        .because("we use `...Utils` (not `...Util`) naming convention")
        .check(productionClasses);
  }
}
