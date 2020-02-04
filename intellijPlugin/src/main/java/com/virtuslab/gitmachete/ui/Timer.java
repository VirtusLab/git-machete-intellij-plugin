package com.virtuslab.gitmachete.ui;

import java.util.Map;
import java.util.TreeMap;

public class Timer {
  private static Map<String, Long> clocks = new TreeMap<>();

  public static void start(String name) {
    clocks.put(name, System.currentTimeMillis());
    System.out.printf("Timer %s started\n", name);
  }

  public static void check(String name) {
    if (clocks.containsKey(name)) {
      System.out.printf("Timer (%s): %d\n", name, System.currentTimeMillis() - clocks.get(name));
    } else {
      System.out.printf("Timer %s is not started\n", name);
    }
  }
}
