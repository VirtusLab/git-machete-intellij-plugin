package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Test;

public class ClassStructureTestSuite extends BaseArchUnitTestSuite {

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

  static class BeReferencedFromOutsideItself extends ArchCondition<JavaClass> {

    BeReferencedFromOutsideItself() {
      super("is not referenced from any other compilation units");
    }

    @Override
    public void check(JavaClass javaClass, ConditionEvents events) {
      Set<JavaAccess<?>> accessesFromOtherCompilationUnits = new HashSet<>(javaClass.getAccessesToSelf());
      accessesFromOtherCompilationUnits.removeAll(javaClass.getAccessesFromSelf());

      if (accessesFromOtherCompilationUnits.isEmpty() && javaClass.getDirectDependenciesToSelf().isEmpty()) {
        String message = javaClass.getDescription() + " is NOT referenced from any other class";
        events.add(new SimpleConditionEvent(javaClass, /* conditionSatisfied */ false, message));
      }
    }
  }

  @SneakyThrows
  private Set<Class<?>> extractAllClassesReferencedFromPluginXmlAttributes() {
    Set<Class<?>> result = new HashSet<>();
    val classLoader = Thread.currentThread().getContextClassLoader();
    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    // Note that there might be multiple plugin.xml files on the classpath,
    // not only from our plugin, but also from the dependencies.
    val resourceUrls = classLoader.getResources("META-INF/plugin.xml");
    while (resourceUrls.hasMoreElements()) {
      val resourceUrl = resourceUrls.nextElement();

      try (val inputStream = resourceUrl.openStream()) {
        val document = documentBuilder.parse(inputStream);
        val xPathfactory = XPathFactory.newInstance();
        val xpath = xPathfactory.newXPath();
        val expr = xpath.compile("//idea-plugin/id/text()");
        // Let's check if the given plugin.xml corresponds to our plugin, or to one of dependencies.
        if (!expr.evaluate(document).equals("com.virtuslab.git-machete"))
          continue;

        val nodeList = document.getElementsByTagName("*");

        for (int i = 0; i < nodeList.getLength(); i++) {
          val node = nodeList.item(i);
          val attributes = node.getAttributes();
          for (int j = 0; j < attributes.getLength(); j++) {
            val attribute = attributes.item(j);
            val maybeFqcn = attribute.getNodeValue();
            try {
              val clazz = Class.forName(maybeFqcn, /* initialize */ false, classLoader);
              result.add(clazz);
            } catch (ClassNotFoundException e) {
              // Not all XML attributes found in plugin.xml correspond to class names,
              // let's ignore those that don't.
            }
          }
        }
      }
    }

    return result;
  }

  @Test
  public void all_classes_should_be_referenced() {
    val classesReferencedFromPluginXmlAttributes = extractAllClassesReferencedFromPluginXmlAttributes().toArray(Class[]::new);
    classes()
        .that().resideOutsideOfPackages(
            // Classes in *.impl.* packages may be instantiated via RuntimeBinding
            "..impl..",
            // For some reason, ArchUnit (com.tngtech.archunit.core.domain.JavaClass.getAccessesFromSelf)
            // doesn't see accesses to static fields
            "com.virtuslab.gitmachete.frontend.defs")
        .and().doNotBelongToAnyOf(classesReferencedFromPluginXmlAttributes)
        // SubtypingBottom is processed by CheckerFramework based on its annotations
        .and().doNotHaveFullyQualifiedName(com.virtuslab.qual.internal.SubtypingBottom.class.getName())
        .should(new BeReferencedFromOutsideItself())
        .check(importedClasses);
  }

  @Test
  public void inner_classes_should_not_be_instantiated_from_constructor_of_enclosing_class() {
    noClasses()
        .that()
        .doNotHaveFullyQualifiedName(com.virtuslab.gitmachete.frontend.actions.dialogs.GitPushDialog.class.getName())
        .and()
        .doNotHaveFullyQualifiedName(com.virtuslab.gitmachete.frontend.ui.impl.root.GitMachetePanel.class.getName())
        .should()
        .callConstructorWhere(
            new DescribedPredicate<JavaConstructorCall>(
                "an inner class is instantiated in a constructor of the enclosing class") {
              @Override
              public boolean test(JavaConstructorCall input) {
                if (input.getOrigin().isConstructor()) {
                  JavaClass constructingClass = input.getOriginOwner();
                  JavaClass constructedClass = input.getTargetOwner();
                  JavaClass parentOfConstructedClass = constructedClass.getEnclosingClass().orElse(null);
                  return constructedClass.isInnerClass() && parentOfConstructedClass == constructingClass;
                } else {
                  return false;
                }
              }
            })
        .because("the enclosing object might not be fully initialized yet when it's passed to inner class constructor. " +
            "See https://github.com/typetools/checker-framework/issues/3407 for details. " +
            "Consider using a static nested class " +
            "and passing a reference to the enclosing object (or to the fields thereof) explicitly")
        .check(importedClasses);
  }
}
