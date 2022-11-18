package com.virtuslab.archunit;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.intellij.ide.util.PropertiesComponent;
import org.junit.Test;

import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

public class ForbiddenMethodsTestSuite extends BaseArchUnitTestSuite {

  @Test
  public void no_classes_should_call_AnActionEvent_getRequiredData() {
    noClasses()
        .should()
        .callMethod(com.intellij.openapi.actionSystem.AnActionEvent.class, "getRequiredData",
            com.intellij.openapi.actionSystem.DataKey.class)
        .because("getRequiredData can fail in the runtime if the requested DataKey is missing; " +
            "use AnActionEvent#getData instead")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_call_Collections_nCopies() {
    noClasses()
        .should().callMethod(java.util.Collections.class, "nCopies", int.class, Object.class)
        .because("it is confusing as it does not copy the objects but just copies the reference N times")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_call_File_exists() {
    noClasses()
        .should().callMethod(java.io.File.class, "exists")
        .because("in most cases, the check you want to do is `isFile` rather than `exists` (what if this is a directory?)")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_call_FileContentUtil_reparseFiles() {
    noClasses()
        .should().callMethodWhere(
            target(owner(assignableTo(com.intellij.util.FileContentUtil.class)))
                .and(target(name("reparseFiles"))))
        .because("com.intellij.util.FileContentUtil#reparseFiles can cause bad performance issues when called with " +
            "includeOpenFiles parameter equal true. Use com.intellij.util.FileContentUtilCore#reparseFiles instead")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_call_FileContentUtil_reparseOpenedFiles() {
    noClasses()
        .should().callMethodWhere(
            target(owner(assignableTo(com.intellij.util.FileContentUtil.class)))
                .and(target(name("reparseOpenedFiles"))))
        .because("com.intellij.util.FileContentUtil#reparseOpenedFiles can cause bad performance issues." +
            "Use com.intellij.util.FileContentUtilCore#reparseFiles instead")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_call_GitRepository_getBranchTrackInfo_s() {
    noClasses()
        .should().callMethod(git4idea.repo.GitRepository.class, "getBranchTrackInfo", String.class)
        .orShould().callMethod(git4idea.repo.GitRepository.class, "getBranchTrackInfos")
        .because("getBranchTrackInfo(s) does not take into account inferred remote " +
            "(when only branch names match but tracking data is unset); " +
            " use tracking data from " + GitMacheteRepositorySnapshot.class.getName() + " instead")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_call_GitUIUtil_notifyError() {
    noClasses()
        .should()
        .callMethodWhere(
            target(owner(assignableTo(git4idea.util.GitUIUtil.class)))
                .and(target(name("notifyError"))))
        .because("due to the bug IDEA-258711, a valid invocation of notifyError can lead to an NPE (see issue #676)")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_call_JComponent_updateUI() {
    noClasses()
        .should().callMethod(javax.swing.JComponent.class, "updateUI")
        .because("repaint() and revalidate() should be used instead, see docs for javax.swing.JComponent for the reasons")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_call_Link_linkSomethingColor() {
    noClasses()
        .should().callMethodWhere(
            target(owner(assignableTo(com.intellij.util.ui.JBUI.CurrentTheme.Link.class)))
                .and(target(name("link.*Color"))))
        .because("links should be avoided since they are unreliable as of IDEA 2020.2")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_call_MessageFormat_format() {
    noClasses()
        .that().areNotAssignableTo(com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.class)
        .should().callMethod(java.text.MessageFormat.class, "format", String.class, Object[].class)
        .because("more restrictive " + GitMacheteBundle.class.getSimpleName() + ".format should be used instead")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_call_Option_toString() {
    noClasses()
        .should().callMethod(io.vavr.control.Option.class, "toString")
        .because("Option#toString is sometimes mistakenly interpolated inside strings (also the user-facing ones), " +
            "whereas in 90% of cases the programmer`s intention " +
            "is to interpolate the contained value aka the result of `.get()` and not the Option itself")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_call_PropertiesComponent_getInstance_without_args() {
    noClasses()
        .should().callMethod(PropertiesComponent.class, "getInstance")
        .because(
            "getInstance without `project` argument gives application-level persistence while we prefer project-level persistence")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_call_String_format() {
    noClasses()
        .should().callMethod(String.class, "format", String.class, Object[].class)
        .because("string interpolation should be used instead (as provided by com.antkorwin:better-strings)")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_call_SwingUtilities_invokeLater() {
    noClasses()
        .should().callMethod(javax.swing.SwingUtilities.class, "invokeLater", Runnable.class)
        .because("UiThreadExecutionUtils.invokeLaterIfNeeded(...) should be used instead. " +
            "See docs for " + com.intellij.openapi.application.ModalityState.class.getName() + " for the reasons")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_call_Value_collect() {
    noClasses()
        .should().callMethodWhere(
            target(owner(assignableTo(io.vavr.Value.class)))
                .and(target(name("collect"))))
        .because("invocation of `collect` on a Vavr Value (including Array, List, Map, Set etc.) " +
            "is almost always avoidable and in many cases indicates that a redundant conversion " +
            "is being performed (like `collect`-ing a List into another List etc.). " +
            "Use dedicated methods like `.toList()`, `.toJavaList()`, `.toSet()` etc. " +
            "You might consider suppressing this error, however, if the Value in question is a Vavr Stream.")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_call_Value_peek() {
    noClasses()
        .should().callMethodWhere(
            target(owner(assignableTo(io.vavr.Value.class)))
                .and(target(name("peek"))))
        .because("io.vavr.collection.<X>#peek and java.util.stream.Stream#peek unexpectedly differ in their behaviour. " +
            "`peek` from Vavr calls the given `Consumer` only for the first element in collection, " +
            "while `peek` from Java `Stream` calls the given `Consumer` for every element. " +
            "To avoid possible bugs, invocation of `peek` on Vavr classes is forbidden. " +
            "Usage of `java.util.stream.Stream#peek` is discouraged but not forbidden " +
            "(since it is pretty useful and alternatives are cumbersome).")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_call_println() {
    noClasses()
        .should().callMethodWhere(name("println"))
        .check(importedClasses);
  }

  // Note that https://checkstyle.sourceforge.io/config_coding.html#CovariantEquals doesn't cover methods defined in interfaces.
  @Test
  public void equals_method_should_have_object_parameter_and_boolean_return_type() {
    methods()
        .that()
        .haveName("equals")
        .should()
        .haveRawParameterTypes(Object.class)
        .andShould()
        .haveRawReturnType(Boolean.TYPE)
        .because("of the reasons outlined in https://www.artima.com/pins1ed/object-equality.html -> pitfall #1")
        .check(importedClasses);
  }
}
