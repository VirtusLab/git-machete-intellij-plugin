package com.virtuslab.archunit;

import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.core.domain.JavaModifier.SYNTHETIC;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import org.junit.jupiter.api.Test;

public class ClassStructureTestSuite extends BaseArchUnitTestSuite {

  @Test
  public void abstract_classes_should_not_declare_LOG_field() {
    classes()
        .that()
        .haveModifier(ABSTRACT)
        .should(new ArchCondition<JavaClass>("not declare a LOG field, but use log() abstract method instead") {
          @Override
          public void check(JavaClass javaClass, ConditionEvents events) {
            // Using getFields() and not getAllFields() to only check declared members.
            // We don't care about potential private logger fields from superclasses.
            javaClass.getFields().forEach(field -> {
              if (field.getName().equals("LOG")) {
                String message = javaClass.getFullName() + " should use `abstract Logger log()` instead of `LOG` field";
                events.add(SimpleConditionEvent.violated(javaClass, message));
              }
            });
          }
        })
        .because("the SLF4J logger name should reflect the name of the concrete class, not an abstract base")
        .check(productionClasses);
  }

  @Test
  public void actions_implementing_DumbAware_should_extend_DumbAwareAction() {
    classes()
        .that().areAssignableTo(com.intellij.openapi.actionSystem.AnAction.class)
        .and().implement(com.intellij.openapi.project.DumbAware.class)
        .should().beAssignableTo(com.intellij.openapi.project.DumbAwareAction.class)
        .because("`extends DumbAwareAction` should be used instead of " +
            "extending `AnAction` and implementing `DumbAware` separately")
        .check(productionClasses);
  }

  static class BeReferencedFromOutsideItself extends ArchCondition<JavaClass> {

    BeReferencedFromOutsideItself() {
      super("be referenced from some other class");
    }

    @Override
    public void check(JavaClass javaClass, ConditionEvents events) {
      Set<JavaAccess<?>> accessesFromOtherClasses = new HashSet<>(javaClass.getAccessesToSelf());
      accessesFromOtherClasses.removeAll(javaClass.getAccessesFromSelf());

      if (accessesFromOtherClasses.isEmpty() && javaClass.getDirectDependenciesToSelf().isEmpty()) {
        String message = javaClass.getDescription() + " is NOT referenced from any other class";
        events.add(SimpleConditionEvent.violated(javaClass, message));
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
        .that().doNotHaveModifier(SYNTHETIC)
        // For some reason, ArchUnit (com.tngtech.archunit.core.domain.JavaClass.getAccessesFromSelf)
        // doesn't see accesses to static fields
        .and().resideOutsideOfPackages("com.virtuslab.gitmachete.frontend.defs")
        .and().doNotBelongToAnyOf(classesReferencedFromPluginXmlAttributes)
        // SubtypingBottom is processed by CheckerFramework based on its annotations
        .and().doNotHaveFullyQualifiedName(com.virtuslab.qual.subtyping.internal.SubtypingBottom.class.getName())
        .should(new BeReferencedFromOutsideItself())
        .check(productionClasses);
  }

  @Test
  public void inner_classes_should_not_be_instantiated_from_constructor_of_enclosing_class() {
    noClasses()
        .that()
        .doNotHaveFullyQualifiedName(com.virtuslab.gitmachete.frontend.actions.dialogs.GitPushDialog.class.getName())
        .and()
        .doNotHaveFullyQualifiedName(com.virtuslab.gitmachete.frontend.ui.impl.root.GitMachetePanel.class.getName())
        .and()
        .doNotHaveFullyQualifiedName(com.virtuslab.gitmachete.frontend.actions.dialogs.SlideInDialog.class.getName())
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
        .check(productionClasses);
  }

  @Test
  public void no_classes_declaring_LOG_field_should_call_log_method() {
    noClasses()
        .that(new DescribedPredicate<JavaClass>("declare `LOG` field") {
          @Override
          public boolean test(JavaClass javaClass) {
            return javaClass.getFields().stream().anyMatch(field -> field.getName().equals("LOG"));
          }
        })
        .should()
        .callMethodWhere(name("log"))
        .because("LOG field should be used explicitly instead")
        .check(productionClasses);
  }

  @Test
  @SneakyThrows
  public void no_classes_should_contain_unprocessed_string_interpolations() {
    Method getConstantPool = Class.class.getDeclaredMethod("getConstantPool");
    getConstantPool.setAccessible(true);

    noClasses()
        .should(new ArchCondition<>("contain unprocessed string interpolations") {
          @SneakyThrows
          @Override
          public void check(JavaClass clazz, ConditionEvents events) {
            val constantPool = getConstantPool.invoke(clazz.reflect());
            int constantPoolSize = (int) constantPool.getClass().getDeclaredMethod("getSize").invoke(constantPool);
            for (int i = 0; i < constantPoolSize; i++) {
              try {
                String constant = (String) constantPool.getClass().getDeclaredMethod("getUTF8At", int.class)
                    .invoke(constantPool, i);
                if (constant.matches("^.*\\$\\{.+}.*$")) {
                  events.add(SimpleConditionEvent.satisfied(clazz, "class " + clazz.getName() + " contains " + constant));
                }
              } catch (InvocationTargetException e) {
                if (!(e.getCause() instanceof IllegalArgumentException)) {
                  // IllegalArgumentException wrapped into InvocationTargetException is expected
                  // when `getUTF8At` is called for an index in constant pool that doesn't correspond to a String.
                  // All other errors are unexpected.
                  throw e;
                }
              }
            }
          }
        })
        .because("it's likely that better-strings annotation processor has not been properly applied on some subproject(s)")
        .check(productionClasses);
  }

  @Test
  public void side_effecting_backgroundables_should_not_access_my_project() {
    noClasses()
        .that()
        .areAssignableTo(com.virtuslab.gitmachete.frontend.actions.backgroundables.SideEffectingBackgroundable.class)
        .should()
        .accessFieldWhere(name("myProject"))
        .because("`myProject` is nullable, which leads to redundant nullness checks; used `project` instead")
        .check(productionClasses);
  }
}
