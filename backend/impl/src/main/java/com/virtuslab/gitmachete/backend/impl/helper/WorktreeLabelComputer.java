package com.virtuslab.gitmachete.backend.impl.helper;

import java.nio.file.Path;
import java.util.Objects;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.collection.SortedSet;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

/**
 * Computes the per-branch worktree labels surfaced as {@link com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot#getWorktreeLabelByLocalBranchName()}.
 *
 * <p>The shape of the result and the labeling rules are documented on that interface method.
 */
public final class WorktreeLabelComputer {
  private static final String THIS_WORKTREE_LABEL = "<this worktree>";
  private static final String MAIN_WORKTREE_LABEL = "<main worktree>";

  private WorktreeLabelComputer() {}

  /**
   * @param worktreeRootByLocalBranchName per-branch worktree root path of the worktree currently
   *        holding that branch; branches not held by any worktree must be absent
   * @param currentWorktreeRoot           the root path of the worktree this snapshot was built against
   * @param mainWorktreeRoot              the root path of the main worktree (= parent of the common
   *                                      git dir); the only worktree if no linked ones exist
   * @return per-branch label, or an empty map if the feature does not fire
   */
  @UIThreadUnsafe
  public static Map<String, String> compute(
      Map<String, Path> worktreeRootByLocalBranchName,
      Path currentWorktreeRoot,
      Path mainWorktreeRoot) {

    // Feature gating: at least one *linked* worktree (i.e. a holder distinct from the main worktree)
    // must exist among the labeled branches. In a plain single-worktree repo the only possible
    // holder is the main worktree, and tagging every branch with `<this worktree>` would just be
    // pure clutter.
    val linkedPaths = worktreeRootByLocalBranchName.values()
        .filter(p -> !p.equals(mainWorktreeRoot))
        .toSortedSet(Path::compareTo);
    if (linkedPaths.isEmpty()) {
      return HashMap.empty();
    }

    val labelByLinkedPath = stripLongestCommonPathPrefix(linkedPaths);

    Map<String, String> result = HashMap.empty();
    for (val tuple : worktreeRootByLocalBranchName) {
      val branch = tuple._1;
      val path = tuple._2;
      String label;
      if (path.equals(currentWorktreeRoot)) {
        label = THIS_WORKTREE_LABEL;
      } else if (path.equals(mainWorktreeRoot)) {
        label = MAIN_WORKTREE_LABEL;
      } else {
        label = labelByLinkedPath.get(path).getOrElse(path::toString);
      }
      result = result.put(branch, label);
    }
    return result;
  }

  /**
   * Returns each input path with the longest leading path-<i>component</i> prefix shared by all
   * inputs stripped. The stripping is always done component-wise (full path segments), never
   * character-wise: {@code ["/a/b/foo", "/a/b/bar"]} collapses to {@code ["foo", "bar"]}, whereas
   * {@code ["/a/proj-foo", "/a/proj-bar"]} collapses to {@code ["proj-foo", "proj-bar"]} (not
   * {@code ["foo", "bar"]}) - the result always names a real directory entry.
   *
   * <p>Edge cases:
   * <ul>
   *   <li>a single input has no shared prefix to speak of; we return its basename so callers always
   *       get a printable label rather than the empty string;</li>
   *   <li>if one input is a prefix of another (e.g. {@code ["/a/b", "/a/b/c"]}), we keep at least
   *       one trailing component for every input - so the shorter one doesn't collapse to the empty
   *       string;</li>
   *   <li>if the inputs share nothing beyond the filesystem root, we keep that root in each result
   *       so the labels remain recognizable absolute paths rather than misleadingly-relative-looking
   *       strings.</li>
   * </ul>
   *
   * <p>Inputs with mismatching filesystem roots are returned as-is (each mapped to its own full
   * path string), since there's no meaningful prefix to strip across them.
   */
  @UIThreadUnsafe
  public static Map<Path, String> stripLongestCommonPathPrefix(SortedSet<Path> paths) {
    if (paths.isEmpty()) {
      return HashMap.empty();
    }
    if (paths.size() == 1) {
      val only = paths.head();
      val name = only.getFileName();
      return HashMap.of(only, name != null ? name.toString() : only.toString());
    }

    val first = paths.head();
    val firstRoot = first.getRoot();
    val allSameRoot = paths.forAll(p -> Objects.equals(p.getRoot(), firstRoot));
    if (!allSameRoot) {
      Map<Path, String> result = HashMap.empty();
      for (val p : paths) {
        result = result.put(p, p.toString());
      }
      return result;
    }

    val minNameCount = paths.map(Path::getNameCount).min().get();
    int uncappedCommonLen = 0;
    for (int i = 0; i < minNameCount; i++) {
      val firstName = first.getName(i);
      final int idx = i;
      val allMatch = paths.forAll(p -> p.getName(idx).equals(firstName));
      if (allMatch) {
        uncappedCommonLen = i + 1;
      } else {
        break;
      }
    }

    // Cap so the shortest doesn't collapse to "" (`["/a/b", "/a/b/c"]` -> `["b", "b/c"]`, not `["", "c"]`).
    int commonLen = Math.min(uncappedCommonLen, minNameCount - 1);

    // Keep the filesystem root on each label only when nothing beyond it is shared - otherwise the
    // stripped tails would look misleadingly relative (`x/foo` vs `/x/foo`). When the cap above
    // drove commonLen down to 0 (e.g. `["/a", "/a/b"]` capped from 1 to 0), we explicitly do NOT
    // re-attach the root: `/a` really is a shared prefix component and ought to be stripped.
    @Nullable Path resultRoot = uncappedCommonLen == 0 ? firstRoot : null;

    Map<Path, String> result = HashMap.empty();
    for (val p : paths) {
      val tail = p.subpath(commonLen, p.getNameCount());
      val s = resultRoot != null ? resultRoot.toString() + tail.toString() : tail.toString();
      result = result.put(p, s);
    }
    return result;
  }
}
