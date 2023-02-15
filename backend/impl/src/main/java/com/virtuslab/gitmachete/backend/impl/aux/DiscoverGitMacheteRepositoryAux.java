package com.virtuslab.gitmachete.backend.impl.aux;

import java.time.Instant;

import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import lombok.CustomLog;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.interning.qual.UsesObjectEquals;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IBranchReference;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.impl.hooks.PreRebaseHookExecutor;
import com.virtuslab.gitmachete.backend.impl.hooks.StatusBranchHookExecutor;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public class DiscoverGitMacheteRepositoryAux extends CreateGitMacheteRepositoryAux {

  private static final String MASTER = "master";
  private static final String MAIN = "main"; // see https://github.com/github/renaming
  private static final String DEVELOP = "develop";

  @UIThreadUnsafe
  public DiscoverGitMacheteRepositoryAux(
      IGitCoreRepository gitCoreRepository,
      StatusBranchHookExecutor statusHookExecutor,
      PreRebaseHookExecutor preRebaseHookExecutor) throws GitCoreException {
    super(gitCoreRepository, statusHookExecutor, preRebaseHookExecutor);
  }

  /**
   * A node of a mutable tree, with extra feature of near-constant time of checking up tree root thanks to path compression.
   * See <a href="https://en.wikipedia.org/wiki/Disjoint-set_data_structure#Find">Disjoint-set data structure on wikipedia</a>.
   */
  @ToString
  @UsesObjectEquals
  private static class CompressablePathTreeNode {
    @Getter
    private final String name;

    @Getter
    private List<CompressablePathTreeNode> children;

    @Getter
    private @Nullable CompressablePathTreeNode parent;

    private @NotOnlyInitialized CompressablePathTreeNode root;

    CompressablePathTreeNode(String name) {
      this.name = name;
      this.children = List.empty();
      this.parent = null;
      this.root = this;
    }

    void attachUnder(CompressablePathTreeNode newParent) {
      parent = newParent;
      root = newParent.root;
    }

    void appendChild(CompressablePathTreeNode newChild) {
      children = children.append(newChild);
    }

    void removeChild(CompressablePathTreeNode child) {
      children = children.remove(child);
    }

    CompressablePathTreeNode getRoot() {
      // The actual path compression happens here.
      if (root != this && root.root != root) {
        root = root.getRoot();
      }
      return root;
    }

    @ToString.Include(name = "children") // avoid recursive `toString` calls on children
    private List<String> getChildNames() {
      return children.map(e -> e.getName());
    }

    @ToString.Include(name = "parent") // avoid recursive `toString` call on parent
    private @Nullable String getParentName() {
      return parent != null ? parent.name : null;
    }

    @ToString.Include(name = "root") // avoid recursive `toString` call on root
    private String getRootName() {
      return root.name;
    }

    public BranchLayoutEntry toBranchLayoutEntry() {
      return new BranchLayoutEntry(name, /* customAnnotation */ null, children.map(c -> c.toBranchLayoutEntry()));
    }
  }

  @UIThreadUnsafe
  private Map<String, Instant> deriveLastCheckoutTimestampByBranchName() throws GitCoreException {
    java.util.Map<String, Instant> result = new java.util.HashMap<>();

    for (val reflogEntry : gitCoreRepository.deriveHead().getReflogFromMostRecent()) {
      val checkoutEntry = reflogEntry.parseCheckout();
      if (checkoutEntry != null) {
        val timestamp = reflogEntry.getTimestamp();
        // `putIfAbsent` since we only care about the most recent occurrence of the given branch being checked out,
        // and we iterate over the reflog starting from the latest entries.
        result.putIfAbsent(checkoutEntry.getFromBranchName(), timestamp);
        result.putIfAbsent(checkoutEntry.getToBranchName(), timestamp);
      }
    }
    return HashMap.ofAll(result);
  }

  @UIThreadUnsafe
  public IGitMacheteRepositorySnapshot discoverLayoutAndCreateSnapshot(int mostRecentlyCheckedOutBranchesCount)
      throws GitMacheteException, GitCoreException {

    List<String> localBranchNames = localBranches.map(lb -> lb.getName());
    List<String> fixedRootBranchNames = List.empty();
    List<String> nonFixedRootBranchNames = localBranchNames;
    if (localBranchNames.contains(MASTER)) {
      fixedRootBranchNames = fixedRootBranchNames.append(MASTER);
      nonFixedRootBranchNames = nonFixedRootBranchNames.remove(MASTER);
    } else if (localBranchNames.contains(MAIN)) {
      fixedRootBranchNames = fixedRootBranchNames.append(MAIN);
      nonFixedRootBranchNames = nonFixedRootBranchNames.remove(MAIN);
    }
    if (localBranchNames.contains(DEVELOP)) {
      fixedRootBranchNames = fixedRootBranchNames.append(DEVELOP);
      nonFixedRootBranchNames = nonFixedRootBranchNames.remove(DEVELOP);
    }
    List<String> freshNonFixedRootBranchNames;

    // Let's only leave at most the given number of most recently checked out ("fresh") branches.
    if (nonFixedRootBranchNames.size() <= mostRecentlyCheckedOutBranchesCount) {
      freshNonFixedRootBranchNames = nonFixedRootBranchNames;
    } else {
      Map<String, Instant> lastCheckoutTimestampByBranchName = deriveLastCheckoutTimestampByBranchName();

      val freshAndStaleNonFixedRootBranchNames = nonFixedRootBranchNames
          .sortBy(branchName -> lastCheckoutTimestampByBranchName.getOrElse(branchName, Instant.MIN))
          .reverse()
          .splitAt(mostRecentlyCheckedOutBranchesCount);
      freshNonFixedRootBranchNames = freshAndStaleNonFixedRootBranchNames._1.sorted();

      LOG.debug(() -> "Skipping stale branches from the discovered layout: "
          + freshAndStaleNonFixedRootBranchNames._2.mkString(", "));
    }

    // Let's use linked maps to ensure a deterministic result.
    Map<String, CompressablePathTreeNode> nodeByFixedRootBranchNames = fixedRootBranchNames
        .toLinkedMap(name -> Tuple.of(name, new CompressablePathTreeNode(name)));
    Map<String, CompressablePathTreeNode> nodeByFreshNonFixedRootBranch = freshNonFixedRootBranchNames
        .toLinkedMap(name -> Tuple.of(name, new CompressablePathTreeNode(name)));
    Map<String, CompressablePathTreeNode> nodeByIncludedBranchName = nodeByFixedRootBranchNames
        .merge(nodeByFreshNonFixedRootBranch);
    LOG.debug(() -> "Branches included in the discovered layout: " + nodeByIncludedBranchName.keySet().mkString(", "));

    // `roots` may be an empty list in the rare case there's no master/main/develop branch in the repository.
    List<CompressablePathTreeNode> roots = nodeByFixedRootBranchNames.values().toList();

    // Skipping the parent inference for fixed roots and for the stale non-fixed-root branches.
    for (val branchNode : nodeByFreshNonFixedRootBranch.values()) {
      // Note that stale non-fixed-root branches are never considered as candidates for the parent.
      Seq<String> parentCandidateNames = nodeByIncludedBranchName.values()
          .filter(e -> e.getRoot() != branchNode)
          .map(e -> e.getName());
      LOG.debug(() -> "Parent candidate(s) for ${branchNode.getName()}: " + parentCandidateNames.mkString(", "));

      IBranchReference parent = inferParentForLocalBranch(parentCandidateNames.toSet(), branchNode.getName());

      if (parent != null) {
        String parentName = parent.getName();
        LOG.debug(() -> "Parent inferred for ${branchNode.getName()} is ${parentName}");

        val parentNode = nodeByIncludedBranchName.get(parentName).getOrNull();
        // Generally we expect an node for parent to always be present.
        if (parentNode != null) {
          branchNode.attachUnder(parentNode);
          parentNode.appendChild(branchNode);
        }
      } else {
        LOG.debug(() -> "No parent inferred for ${branchNode.getName()}; attaching as new root");

        roots = roots.append(branchNode);
      }
    }

    val NL = System.lineSeparator();
    LOG.debug(() -> "Final discovered entries: " + NL + nodeByIncludedBranchName.values().mkString(NL));

    // Post-process the discovered layout to remove the branches that would both:
    // 1. have no child AND
    // 2. be merged to their respective parents.
    for (val branchNode : nodeByFreshNonFixedRootBranch.values()) {
      if (branchNode.getChildren().nonEmpty()) {
        continue;
      }

      val parentNode = branchNode.getParent();
      if (parentNode == null) {
        // This will happen for the roots of the discovered layout.
        continue;
      }
      val branch = localBranchByName.get(branchNode.getName()).getOrNull();
      val parentBranch = localBranchByName.get(parentNode.getName()).getOrNull();
      if (branch == null || parentBranch == null) {
        // This should never happen.
        continue;
      }

      // A little hack wrt. fork point: we only want to distinguish between a branch merged or not merged to the parent,
      // and fork point does not affect this specific distinction.
      // It's in fact only useful for distinguishing between `InSync` and `InSyncButForkPointOff`,
      // but here we don't care if the former is returned instead of the latter.
      SyncToParentStatus syncStatus = deriveSyncToParentStatus(branch, parentBranch, /* forkPoint */ null);
      if (syncStatus == SyncToParentStatus.MergedToParent) {
        LOG.debug(() -> "Removing node for ${branchNode.getName()} " +
            "since it's merged to its parent ${parentNode.getName()} and would have no children");
        parentNode.removeChild(branchNode);
      }
    }
    return createSnapshot(new BranchLayout(roots.map(r -> r.toBranchLayoutEntry())));
  }

}
