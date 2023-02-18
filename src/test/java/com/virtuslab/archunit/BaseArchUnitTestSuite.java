package com.virtuslab.archunit;

import java.util.function.BiPredicate;

import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.vavr.collection.List;
import io.vavr.control.Option;
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

  protected static ArchCondition<JavaCodeUnit> callAnyCodeUnitsThat(String description,
      BiPredicate<? super JavaCodeUnit, AccessTarget.CodeUnitCallTarget> predicate) {
    return new ArchCondition<JavaCodeUnit>("call code units that " + description) {
      @Override
      public void check(JavaCodeUnit codeUnit, ConditionEvents events) {
        codeUnit.getCallsFromSelf().forEach(call -> {
          val callTarget = call.getTarget();
          if (predicate.test(codeUnit, callTarget)) {
            String message = codeUnit.getFullName() + " calls " + callTarget.getFullName();
            events.add(SimpleConditionEvent.satisfied(codeUnit, message));
          }
        });
      }
    };
  }

  protected static ArchCondition<JavaCodeUnit> callAtLeastOnceACodeUnitThat(String description,
      BiPredicate<? super JavaCodeUnit, AccessTarget.CodeUnitCallTarget> predicate) {
    return new ArchCondition<JavaCodeUnit>("call a code unit that " + description) {
      @Override
      public void check(JavaCodeUnit codeUnit, ConditionEvents events) {
        if (codeUnit.getCallsFromSelf().stream().noneMatch(call -> predicate.test(codeUnit, call.getTarget()))) {
          String message = "code unit ${codeUnit.getFullName()} does not call any code unit that " + description;
          events.add(SimpleConditionEvent.violated(codeUnit, message));
        }
      }
    };
  }

  protected static ArchCondition<JavaMethod> overrideAnyMethodThat(String description,
      BiPredicate<JavaMethod, JavaMethod> predicate) {
    return new ArchCondition<JavaMethod>("override any method that " + description) {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        JavaClass owner = method.getOwner();
        val superTypes = List.ofAll(owner.getAllRawInterfaces()).appendAll(owner.getAllRawSuperclasses());
        val paramTypeNames = method.getParameters().stream().map(p -> p.getRawType().getFullName()).toArray(String[]::new);
        val overriddenMethods = superTypes
            .flatMap(s -> Option.ofOptional(s.tryGetMethod(method.getName(), paramTypeNames)));

        for (val overriddenMethod : overriddenMethods) {
          if (predicate.test(method, overriddenMethod)) {
            String message = method.getFullName() + " overrides " + overriddenMethod.getFullName();
            events.add(SimpleConditionEvent.satisfied(method, message));
          }
        }
      }
    };
  }
}
