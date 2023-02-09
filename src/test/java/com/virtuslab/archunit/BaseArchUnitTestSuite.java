package com.virtuslab.archunit;

import java.util.function.BiPredicate;

import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import lombok.val;

public class BaseArchUnitTestSuite {
  protected static final JavaClasses productionClasses = new ClassFileImporter()
      .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
      // UI Tests aren't covered by the predefined ArchUnit pattern for Gradle test class locations.
      // Note that :test Gradle configuration should NOT see classes from :uiTest...
      // but gradle-intellij-plugin v1.8.0 "added `sourceSets` output directories to the classpath of the `test` task".
      // See https://github.com/JetBrains/gradle-intellij-plugin/commit/9530b23838b98dcab9cd854ecea1ef9ec20193a8
      .withImportOption(location -> !location.contains("/build/classes/scala/uiTest/"))
      .importPackages("com.virtuslab");

  protected static ArchCondition<JavaMethod> callAnyMethodsThat(String description,
      BiPredicate<? super JavaMethod, AccessTarget.MethodCallTarget> predicate) {
    return new ArchCondition<JavaMethod>("call methods that " + description) {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        method.getMethodCallsFromSelf().forEach(call -> {
          val calledMethod = call.getTarget();
          if (predicate.test(method, calledMethod)) {
            events.add(SimpleConditionEvent.satisfied(method, method.getFullName() + " calls " + calledMethod.getFullName()));
          }
        });
      }
    };
  }

  protected static ArchCondition<JavaMethod> callAtLeastOnceAMethodThat(String description,
      BiPredicate<? super JavaMethod, AccessTarget.MethodCallTarget> predicate) {
    return new ArchCondition<JavaMethod>("call a method that " + description) {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        if (method.getMethodCallsFromSelf().stream().noneMatch(call -> predicate.test(method, call.getTarget()))) {
          String message = "method ${method.getFullName()} does not call any method that " + description;
          events.add(SimpleConditionEvent.violated(method, message));
        }
      }
    };
  }
}
