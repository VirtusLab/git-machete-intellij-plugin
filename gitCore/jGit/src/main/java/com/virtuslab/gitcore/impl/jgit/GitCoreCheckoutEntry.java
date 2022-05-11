package com.virtuslab.gitcore.impl.jgit;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.lib.CheckoutEntry;

import com.virtuslab.gitcore.api.IGitCoreCheckoutEntry;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GitCoreCheckoutEntry implements IGitCoreCheckoutEntry {
  private final String fromBranchName;
  private final String toBranchName;

  static GitCoreCheckoutEntry of(CheckoutEntry checkoutEntry) {
    return new GitCoreCheckoutEntry(checkoutEntry.getFromBranch(), checkoutEntry.getToBranch());
  }
}
