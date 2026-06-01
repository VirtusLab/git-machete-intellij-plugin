package com.virtuslab.gitmachete.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Comparator;

import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.SortedSet;
import io.vavr.collection.TreeSet;
import org.junit.jupiter.api.Test;

import com.virtuslab.gitmachete.backend.impl.helper.WorktreeLabelComputer;

public class WorktreeLabelComputerUnitTestSuite {

  private static final Path MAIN = Path.of("/repos/proj/main");
  private static final Path WT_A = Path.of("/repos/proj/wt-a");
  private static final Path WT_B = Path.of("/repos/proj/wt-b");

  @Test
  public void emptyWhenNoLinkedWorktrees() {
    // No linked worktree => feature does not fire, even if every branch is in the main worktree.
    Map<String, Path> input = HashMap.<String, Path>empty().put("master", MAIN).put("develop", MAIN);

    Map<String, String> result = WorktreeLabelComputer.compute(input, /* current */ MAIN, /* main */ MAIN);

    assertEquals(HashMap.empty(), result);
  }

  @Test
  public void labelsThisWorktreeMainWorktreeAndStrippedLinkedLabels() {
    Map<String, Path> input = HashMap.<String, Path>empty()
        .put("master", MAIN)
        .put("feature-a", WT_A)
        .put("feature-b", WT_B);

    Map<String, String> result = WorktreeLabelComputer.compute(input, /* current */ MAIN, /* main */ MAIN);

    assertEquals(
        HashMap.<String, String>empty()
            .put("master", "<this worktree>")
            .put("feature-a", "wt-a")
            .put("feature-b", "wt-b"),
        result);
  }

  @Test
  public void labelsMainWorktreeWhenCurrentIsLinked() {
    Map<String, Path> input = HashMap.<String, Path>empty()
        .put("master", MAIN)
        .put("feature-a", WT_A)
        .put("feature-b", WT_B);

    // We are now standing in `wt-a` (a linked worktree).
    Map<String, String> result = WorktreeLabelComputer.compute(input, /* current */ WT_A, /* main */ MAIN);

    assertEquals(
        HashMap.<String, String>empty()
            .put("master", "<main worktree>")
            .put("feature-a", "<this worktree>")
            .put("feature-b", "wt-b"),
        result);
  }

  @Test
  public void mainWorktreeDoesNotInflateLinkedPrefix() {
    // Linked worktrees live under `/tmp/wts`; main lives under `/home/user`. The main worktree must
    // be excluded from the prefix computation, otherwise the only shared component across {main,
    // linked-A, linked-B} would be `/` and the linked labels would degrade to full absolute paths.
    Path tmpA = Path.of("/tmp/wts/foo");
    Path tmpB = Path.of("/tmp/wts/bar");
    Path home = Path.of("/home/user/proj");
    Map<String, Path> input = HashMap.<String, Path>empty()
        .put("master", home)
        .put("feature-a", tmpA)
        .put("feature-b", tmpB);

    Map<String, String> result = WorktreeLabelComputer.compute(input, /* current */ home, /* main */ home);

    assertEquals(
        HashMap.<String, String>empty()
            .put("master", "<this worktree>")
            .put("feature-a", "foo")
            .put("feature-b", "bar"),
        result);
  }

  @Test
  public void stripLongestCommonPathPrefix_collapsesPerComponent() {
    // Component-wise stripping: `proj-foo` and `proj-bar` share the *parent* component but not the
    // string prefix `proj-`, so the result is the basenames in full - never the character prefix.
    Path foo = Path.of("/a/proj-foo");
    Path bar = Path.of("/a/proj-bar");

    Map<Path, String> result = WorktreeLabelComputer.stripLongestCommonPathPrefix(sortedPaths(foo, bar));

    assertEquals(HashMap.<Path, String>empty().put(foo, "proj-foo").put(bar, "proj-bar"), result);
  }

  @Test
  public void stripLongestCommonPathPrefix_keepsTrailingComponentWhenOneIsPrefixOfOther() {
    // `/a/b` is a prefix of `/a/b/c`; without the cap the shorter path would collapse to "".
    Path shorter = Path.of("/a/b");
    Path longer = Path.of("/a/b/c");

    Map<Path, String> result = WorktreeLabelComputer.stripLongestCommonPathPrefix(sortedPaths(shorter, longer));

    assertEquals(HashMap.<Path, String>empty().put(shorter, "b").put(longer, "b/c"), result);
  }

  @Test
  public void stripLongestCommonPathPrefix_keepsFilesystemRootWhenItIsTheOnlyShared() {
    // When the only shared component is `/`, we keep it in each result so the labels still look
    // like absolute paths instead of misleadingly-relative-looking strings.
    Path x = Path.of("/x/foo");
    Path y = Path.of("/y/bar");

    Map<Path, String> result = WorktreeLabelComputer.stripLongestCommonPathPrefix(sortedPaths(x, y));

    assertEquals(HashMap.<Path, String>empty().put(x, "/x/foo").put(y, "/y/bar"), result);
  }

  @Test
  public void stripLongestCommonPathPrefix_singleInputReturnsBasename() {
    Path only = Path.of("/a/b/c");

    Map<Path, String> result = WorktreeLabelComputer.stripLongestCommonPathPrefix(sortedPaths(only));

    assertEquals(HashMap.<Path, String>empty().put(only, "c"), result);
  }

  private static SortedSet<Path> sortedPaths(Path... paths) {
    return TreeSet.ofAll(Comparator.<Path>naturalOrder(), List.of(paths));
  }
}
