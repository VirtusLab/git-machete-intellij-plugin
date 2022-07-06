package com.virtuslab.gitmachete.resourcebundles.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.*;
import java.util.Properties;

import org.junit.Test;

public class GitMacheteBundlePropertiesTestSuite {

  private final String macheteBundleProperties = "GitMacheteBundle.properties";

  @Test
  public void htmlProperties_should_have_correct_syntax() throws IOException {

    Properties property = new Properties();
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    property.load(loader.getResourceAsStream(macheteBundleProperties));

    property.entrySet().stream()
        .filter(prop -> ((String) prop.getValue()).contains("<"))
        .forEach(t -> {
          String key = (String) t.getKey();
          String value = (String) t.getValue();
          assertEquals("HTML property should start with <html>", "<html>", value.substring(0, 6));
          assertEquals("HTML property should end with </html>", "</html>", value.substring(value.length() - 7));
          assertEquals("HTML property key should have a .HTML suffix", ".HTML", key.substring(key.length() - 5));
        });
  }

  @Test
  public void properties_should_have_no_double_quote_wrapping() throws IOException {

    Properties property = new Properties();
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    property.load(loader.getResourceAsStream(macheteBundleProperties));

    property.entrySet().stream()
        .filter(prop -> ((String) prop.getValue()).contains("\""))
        .forEach(t -> {
          String key = (String) t.getKey();
          String value = (String) t.getValue();
          assertFalse(
              "Key '${key}' has value wrapped in double quotes (\"). Remove unnecessary wrapping",
              value.endsWith("\"") && value.startsWith("\""));
        });
  }

  @Test
  public void properties_should_contain_only_valid_single_quotes() throws IOException {

    Properties property = new Properties();
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    property.load(loader.getResourceAsStream(macheteBundleProperties));

    property.entrySet().stream()
        .forEach(t -> {
          String key = (String) t.getKey();
          String value = (String) t.getValue();
          assertFalse(
              "Key '${key}' has a format element ({number}), but contains single quote (')." +
                  "Use double single quote instead ('')",
              value.matches(".*\\{\\d+}.*") && value.matches(".*((^'[^'])|([^']'[^'])|([^']'$)).*"));
          assertFalse(
              "Key '${key}' has NO format element ({number}), but contains double single quote ('')." +
                  "Use single quote instead (')",
              !value.matches(".*\\{\\d+}.*") && value.matches(".*''.*"));
        });
  }
}
