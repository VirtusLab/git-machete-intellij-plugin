package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.Set;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.backend.impl.aux.Aux;
import com.virtuslab.gitmachete.backend.impl.aux.CreateGitMacheteRepositoryAux;
import com.virtuslab.gitmachete.backend.impl.aux.DiscoverGitMacheteRepositoryAux;
import com.virtuslab.gitmachete.backend.impl.hooks.PreRebaseHookExecutor;
import com.virtuslab.gitmachete.backend.impl.hooks.StatusBranchHookExecutor;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@RequiredArgsConstructor
public class GitMacheteRepository implements IGitMacheteRepository {

  private final IGitCoreRepository gitCoreRepository;
  private final StatusBranchHookExecutor statusHookExecutor;
  private final PreRebaseHookExecutor preRebaseHookExecutor;

  private static final int NUMBER_OF_MOST_RECENTLY_CHECKED_OUT_BRANCHES_FOR_DISCOVER = 10;

  @Override
  @UIThreadUnsafe
  public IGitMacheteRepositorySnapshot createSnapshotForLayout(BranchLayout branchLayout) throws GitMacheteException {
    if (javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      if (!stackTrace.contains("at com.virtuslab.gitmachete.frontend.actions.toolbar.DiscoverAction.actionPerformed")) {
        System.out.println("Expected non-EDT:");
        System.out.println(stackTrace);
        throw new RuntimeException("Expected EDT: " + stackTrace);
      }
    }
    try {
      val aux = new CreateGitMacheteRepositoryAux(gitCoreRepository, statusHookExecutor, preRebaseHookExecutor);
      return aux.createSnapshot(branchLayout);
    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  @Override
  @UIThreadUnsafe
  public @Nullable ILocalBranchReference inferParentForLocalBranch(
      Set<String> eligibleLocalBranchNames,
      String localBranchName) throws GitMacheteException {
    if (javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      if (!stackTrace.contains("at com.virtuslab.gitmachete.frontend.actions.toolbar.DiscoverAction.actionPerformed")) {
        System.out.println("Expected non-EDT:");
        System.out.println(stackTrace);
        throw new RuntimeException("Expected EDT: " + stackTrace);
      }
    }
    try {
      val aux = new Aux(gitCoreRepository);
      return aux.inferParentForLocalBranch(eligibleLocalBranchNames, localBranchName);
    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  @Override
  @UIThreadUnsafe
  public IGitMacheteRepositorySnapshot discoverLayoutAndCreateSnapshot() throws GitMacheteException {
    if (javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      if (!stackTrace.contains("at com.virtuslab.gitmachete.frontend.actions.toolbar.DiscoverAction.actionPerformed")) {
        System.out.println("Expected non-EDT:");
        System.out.println(stackTrace);
        throw new RuntimeException("Expected EDT: " + stackTrace);
      }
    }
    try {
      val aux = new DiscoverGitMacheteRepositoryAux(gitCoreRepository, statusHookExecutor, preRebaseHookExecutor);
      return aux.discoverLayoutAndCreateSnapshot(NUMBER_OF_MOST_RECENTLY_CHECKED_OUT_BRANCHES_FOR_DISCOVER);
    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

}
