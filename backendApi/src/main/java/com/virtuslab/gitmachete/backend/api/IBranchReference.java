package com.virtuslab.gitmachete.backend.api;

import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.RequiresQualifier;

import com.virtuslab.qual.gitmachete.backend.api.ConfirmedLocal;
import com.virtuslab.qual.gitmachete.backend.api.ConfirmedRemote;

/**
 * The only criterion for equality of any instances of any class implementing this interface
 * is equality of {@link #getFullName()}
 */
public interface IBranchReference {
  String getName();

  String getFullName();

  @EnsuresQualifierIf(expression = "this", result = true, qualifier = ConfirmedLocal.class)
  @EnsuresQualifierIf(expression = "this", result = false, qualifier = ConfirmedRemote.class)
  boolean isLocal();

  @EnsuresQualifierIf(expression = "this", result = true, qualifier = ConfirmedRemote.class)
  @EnsuresQualifierIf(expression = "this", result = false, qualifier = ConfirmedLocal.class)
  default boolean isRemote() {
    return !isLocal();
  }

  @RequiresQualifier(expression = "this", qualifier = ConfirmedLocal.class)
  ILocalBranchReference asLocal();

  @RequiresQualifier(expression = "this", qualifier = ConfirmedRemote.class)
  IRemoteTrackingBranchReference asRemote();

  @EnsuresNonNullIf(expression = "#2", result = true)
  static boolean defaultEquals(@FindDistinct IBranchReference self, @Nullable Object other) {
    if (self == other) {
      return true;
    } else if (!(other instanceof IBranchReference)) {
      return false;
    } else {
      return self.getFullName().equals(((IBranchReference) other).getFullName());
    }
  }

  static int defaultHashCode(IBranchReference self) {
    return self.getFullName().hashCode();
  }
}
