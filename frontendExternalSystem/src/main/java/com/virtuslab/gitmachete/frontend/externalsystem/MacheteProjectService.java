package com.virtuslab.gitmachete.frontend.externalsystem;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.util.SmartList;

@Service
public final class MacheteProjectService {
  private final SmartList<IListener> myServiceListeners = new SmartList<>();

  private final Project project;

  public MacheteProjectService(Project project) {
    this.project = project;
  }

  void foo() {
    fireReloadScheduled();
    System.out.println(":tada:");
    fireReloadCompleted();
  }

  public void addServiceListener(IListener listener) {
    myServiceListeners.add(listener);
  }

  public void fireReloadCompleted() {
    for (IListener each : myServiceListeners) {
      each.reloadCompleted();
    }
  }

  public void fireReloadScheduled() {
    for (IListener each : myServiceListeners) {
      each.reloadScheduled();
    }
  }

  public interface IListener {
    default void reloadCompleted() {}

    default void reloadScheduled() {}
  }
}
