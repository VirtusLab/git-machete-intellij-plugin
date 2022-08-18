package com.virtuslab.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

public class BaseArchUnitTestSuite {
  protected static final JavaClasses importedClasses = new ClassFileImporter()
      .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
      // UI Tests aren't covered by the predefined ArchUnit pattern for Gradle test class locations.
      // Note that :test Gradle configuration should NOT see classes from :uiTest...
      // but gradle-intellij-plugin v1.8.0 "added `sourceSets` output directories to the classpath of the `test` task".
      // See https://github.com/JetBrains/gradle-intellij-plugin/commit/9530b23838b98dcab9cd854ecea1ef9ec20193a8
      .withImportOption(location -> !location.contains("/build/classes/scala/uiTest/"))
      .importPackages("com.virtuslab");
}
