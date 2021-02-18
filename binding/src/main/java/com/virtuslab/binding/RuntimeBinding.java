package com.virtuslab.binding;

import java.util.stream.Collectors;

import lombok.CustomLog;
import lombok.SneakyThrows;
import lombok.val;
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
  @SneakyThrows
  public static <T> T instantiateSoleImplementingClass(Class<T> interfaze) {
    java.util.Set<Class<? extends T>> classes = reflectionsInstance.getSubTypesOf(interfaze).stream()
        .filter(c -> !c.isInterface() && !c.isAnonymousClass() && !c.isLocalClass() && !c.isMemberClass())
        .collect(Collectors.toSet());
    if (classes.isEmpty()) {
      // For some obscure reason, string interpolation & Lombok don't work together in this specific class.
      throw new ClassNotFoundException("No viable class implementing " + interfaze.getCanonicalName() + " found");
    }
    if (classes.size() > 1) {
      val classesString = String.join(", ",
          classes.stream().map(c -> String.valueOf(c.getCanonicalName())).collect(Collectors.toSet()));
      throw new ClassNotFoundException(
          "More than one viable class implementing " + interfaze.getCanonicalName() + " found: " + classesString);
    }

    val soleImplementingClass = classes.iterator().next();
    LOG.debug(() -> "Binding " + interfaze.getCanonicalName() + " to " + soleImplementingClass.getCanonicalName());
    return soleImplementingClass.getDeclaredConstructor().newInstance();
  }
}
