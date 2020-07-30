package com.virtuslab.branchlayout.api;

public class EntryIsDescendantOfException extends BranchLayoutException {
  private final IBranchLayoutEntry descendant;
  private final IBranchLayoutEntry ancestor;

  public EntryIsDescendantOfException(String message, IBranchLayoutEntry descendant, IBranchLayoutEntry ancestor) {
    super(message);
    this.descendant = descendant;
    this.ancestor = ancestor;
  }
}
