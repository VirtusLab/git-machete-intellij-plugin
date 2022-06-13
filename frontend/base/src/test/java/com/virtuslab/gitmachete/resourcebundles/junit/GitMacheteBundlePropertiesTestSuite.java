package com.virtuslab.gitmachete.resourcebundles.junit;

import static org.junit.Assert.assertTrue;

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
        .forEach(t -> assertTrue(isCorrectHtmlPropertyFormat((String) t.getKey(), (String) t.getValue())));
  }

  private boolean isCorrectHtmlPropertyFormat(String key, String value) {
    if (value.contains(Character.toString('<'))) {
      String beginHtmlTag = value.substring(0, 6);
      String endHtmlTag = value.substring(value.length() - 7);
      String htmlSuffix = key.substring(key.length() - 5);
      return beginHtmlTag.equals("<html>") && endHtmlTag.equals("</html>") && htmlSuffix.equals(".HTML");
    }
    return true;
  }
}
