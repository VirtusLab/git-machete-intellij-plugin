package com.virtuslab.binding;

import static lombok.Lombok.sneakyThrow;

import java.util.stream.Collectors;

import lombok.CustomLog;
import org.reflections.Reflections;

@CustomLog
public final class RuntimeBinding {
  private RuntimeBinding() {}

  private static final Reflections reflectionsInstance = new Reflections("com.virtuslab");

  /**
   * @param <T> base interface type
   * @param interfaze {@link Class} object for the base interface
   * @return an instance of the sole non-anonymous/non-local/non-member class implementing {@code interfaze}
   *
   * Sneaky-throws the following exceptions:
   * <ul>
   *  <li>{@link ClassNotFoundException} if zero or more than one non-anonymous/non-local/non-member implementing classes found</li>
   *  <li>{@link NoSuchMethodException} if the sole implementing class has no parameterless constructor</li>
   *  <li>{@link IllegalAccessException} if the parameterless constructor of sole implementing class is not accessible</li>
   * </ul>
   */
  public static <T> T instantiateSoleImplementingClass(Class<T> interfaze) {
    try {
      java.util.Set<Class<? extends T>> classes = reflectionsInstance.getSubTypesOf(interfaze).stream()
          .filter(c -> !c.isInterface() && !c.isAnonymousClass() && !c.isLocalClass() && !c.isMemberClass())
          .collect(Collectors.toSet());
      if (classes.isEmpty()) {
        throw new ClassNotFoundException("No viable class implementing ${interfaze.getCanonicalName()} found");
      }
      if (classes.size() > 1) {
        var classesString = String.join(", ",
            classes.stream().map(c -> String.valueOf(c.getCanonicalName())).collect(Collectors.toSet()));
        throw new ClassNotFoundException(
            "More than one viable class implementing ${interfaze.getCanonicalName()} found: ${classesString}");
      }

      var soleImplementingClass = classes.iterator().next();
      LOG.debug(() -> "Binding ${interfaze.getCanonicalName()} to ${soleImplementingClass.getCanonicalName()}");
      return soleImplementingClass.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw sneakyThrow(e);
    }
  }
}
