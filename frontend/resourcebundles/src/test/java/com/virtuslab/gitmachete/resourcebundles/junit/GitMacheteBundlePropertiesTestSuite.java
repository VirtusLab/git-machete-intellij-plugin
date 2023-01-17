package com.virtuslab.gitmachete.resourcebundles.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Properties;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class GitMacheteBundlePropertiesTestSuite {

  private static final String macheteBundleProperties = "GitMacheteBundle.properties";

  @SneakyThrows
  private static Properties loadProperties() {
    Properties properties = new Properties();
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    properties.load(loader.getResourceAsStream(macheteBundleProperties));

    return properties;
  }

  @Test
  public void html_properties_should_have_correct_syntax() {

    Properties properties = loadProperties();

    properties.entrySet().stream()
        .filter(prop -> ((String) prop.getValue()).contains("<"))
        .forEach(t -> {
          String key = (String) t.getKey();
          String value = (String) t.getValue();
          assertEquals("<html>", value.substring(0, 6), "HTML property should start with <html> (key=${key})");
          assertEquals("</html>", value.substring(value.length() - 7), "HTML property should end with </html> (key=${key})");
          assertEquals(".HTML", key.substring(key.length() - 5), "HTML property key should have a .HTML suffix (key=${key})");
        });
  }

  @Test
  public void properties_should_have_no_double_quote_wrapping() {

    Properties properties = loadProperties();

    properties.forEach((keyObj, valueObj) -> {
      String key = (String) keyObj;
      String value = (String) valueObj;
      assertFalse(value.endsWith("\"") && value.startsWith("\""),
          "Key '${key}' has value wrapped in double quotes (\"). Remove unnecessary wrapping");
    });
  }

  @Test
  public void properties_should_contain_only_valid_single_quotes() {

    Properties properties = loadProperties();

    properties.forEach((keyObj, valueObj) -> {
      String key = (String) keyObj;
      String value = (String) valueObj;
      assertFalse(value.matches(".*\\{\\d+}.*") && value.matches("^'[^'].*|.*[^']'[^'].*|.*[^']'$"),
          "Key '${key}' has a format element ({number}), but contains a single apostrophe (')." +
              "Use a double apostrophe ('') instead");
      assertFalse(!value.matches(".*\\{\\d+}.*") && value.matches(".*''.*"),
          "Key '${key}' has NO format element ({number}), but contains a double apostrophe ('')." +
              "Use a single apostrophe (') instead");
    });
  }

  @Test
  public void properties_should_use_ellipsis_instead_of_three_dots() {

    Properties properties = loadProperties();

    properties.entrySet().stream()
        .filter(prop -> ((String) prop.getValue()).contains("..."))
        .forEach(t -> {
          String key = (String) t.getKey();
          fail("Properties should use ellipsis (\\u2026) instead of three dots (key=${key})");
        });
  }
}
