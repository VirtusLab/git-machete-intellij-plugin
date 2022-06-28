package com.virtuslab.gitmachete.resourcebundles.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.*;
import java.util.Properties;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class GitMacheteBundlePropertiesTestSuite {

  private final String macheteBundleProperties = "GitMacheteBundle.properties";

  @Test
  public void html_properties_should_have_correct_syntax() throws IOException {

    Properties property = getProperties();

    property.entrySet().stream()
        .filter(prop -> ((String) prop.getValue()).contains("<"))
        .forEach(t -> {
          String key = (String) t.getKey();
          String value = (String) t.getValue();
          assertEquals("HTML property should start with <html> (key=${key})", "<html>", value.substring(0, 6));
          assertEquals("HTML property should end with </html> (key=${key})", "</html>", value.substring(value.length() - 7));
          assertEquals("HTML property key should have a .HTML suffix (key=${key})", ".HTML", key.substring(key.length() - 5));
        });
  }

  @Test
  public void properties_should_use_ellipsis_instead_of_three_dots() throws IOException {

    Properties properties = getProperties();

    properties.entrySet().stream()
        .filter(prop -> ((String) prop.getValue()).contains("..."))
        .forEach(t -> {
          String key = (String) t.getKey();
          fail("Properties should use ellipsis (\\u2026) instead of three dots (key=${key})");
        });

  }

  @NotNull
  private Properties getProperties() throws IOException {
    Properties property = new Properties();
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    property.load(loader.getResourceAsStream(macheteBundleProperties));
    return property;
  }
}
