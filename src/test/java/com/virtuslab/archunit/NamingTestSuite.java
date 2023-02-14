package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;

public class NamingTestSuite extends BaseArchUnitTestSuite {

  @Test
  public void ApplicationException() {
    classes()
        .that().areAssignableTo(Exception.class)
        .should().haveSimpleNameEndingWith("Exception")
        .check(productionClasses);
  }

  @Test
  public void IParser() {
    // Checking for @Target seems the best available way to identify annotations
    classes()
        .that().areInterfaces()
        .and().resideOutsideOfPackage("com.virtuslab.gitmachete.frontend.file.grammar")
        .and().areNotAnnotatedWith(java.lang.annotation.Target.class)
        .should().haveSimpleNameStartingWith("I")
        .check(productionClasses);
  }
  @Test
  public void ServiceManager() {
    noClasses()
        .should().haveSimpleNameEndingWith("Manager")
        .because("classes called `...Manager` are an indicator of poor design; " +
            "likely a redesign (and not just a rename) is needed")
        .check(productionClasses);
  }

  @Test
  public void StringUtil() {
    noClasses()
        .should().haveSimpleNameEndingWith("Util")
        .because("we use `...Utils` (not `...Util`) naming convention")
        .check(productionClasses);
  }
}
