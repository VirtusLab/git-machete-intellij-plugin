package com.virtuslab.gitmachete.frontend.actions.common;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.collection.SortedSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.checkerframework.checker.tainting.qual.Untainted;

@Service
@SuppressWarnings("regexp") // to allow for `synchronized`
public final class SideEffectingActionTrackingService {

  // Let's compare action ids by reference, in case there are multiple actions with the same name going on.
  // We don't want that finishing one of this actions is marked as if all of them completed.
  @RequiredArgsConstructor
  static class SideEffectiveActionId {
    @Getter
    private final @Untainted String name;

    @Override
    public String toString() {
      return name;
    }
  }

  private Set<SideEffectiveActionId> ongoingActions = HashSet.empty();

  public SideEffectingActionTrackingService(Project project) {}

  public synchronized SideEffectingActionClosable register(@Untainted String id) {
    val actionId = new SideEffectiveActionId(id);
    ongoingActions = ongoingActions.add(actionId);
    return new SideEffectingActionClosable(actionId);
  }

  public synchronized SortedSet<String> getOngoingActions() {
    return ongoingActions.map(a -> a.toString()).toSortedSet();
  }

  @RequiredArgsConstructor
  public class SideEffectingActionClosable implements AutoCloseable {
    private final SideEffectiveActionId actionId;

    @Override
    public void close() {
      synchronized (SideEffectingActionTrackingService.this) {
        ongoingActions = ongoingActions.remove(actionId);
      }
    }
  }
}
