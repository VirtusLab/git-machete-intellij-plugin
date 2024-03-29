package com.virtuslab.branchlayout.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vavr.collection.List;
import lombok.val;
import org.junit.jupiter.api.Test;

public class BranchLayoutTestSuite {

  @Test
  public void shouldBeAbleToFindNextAndPreviousBranches() {
    // given
    String rootName = "root";
    String parentName0 = "parent0";
    String childName0 = "child0";
    String childName1 = "child1";
    String parentName1 = "parent1";
    String childName2 = "child2";

    /*-
        root                          root
          parent0
            child0                      child0
            child1                      child1
          parent1
            child2
    */

    List<BranchLayoutEntry> childBranches0 = List.of(
        new BranchLayoutEntry(childName0, /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry(childName1, /* customAnnotation */ null, List.empty()));

    val entry0 = new BranchLayoutEntry(parentName0, /* customAnnotation */ null, childBranches0);

    List<BranchLayoutEntry> childBranches1 = List.of(
        new BranchLayoutEntry(childName2, /* customAnnotation */ null, List.empty()));

    val entry1 = new BranchLayoutEntry(parentName1, /* customAnnotation */ null, childBranches1);
    val rootEntry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, List.of(entry0, entry1));
    val branchLayout = new BranchLayout(List.of(rootEntry));
    //then
    assertEquals(parentName1, branchLayout.findNextEntry(childName1).getName());
    assertEquals(childName0, branchLayout.findPreviousEntry(childName1).getName());
    assertEquals(childName0, branchLayout.findNextEntry(parentName0).getName());
    assertEquals(parentName0, branchLayout.findPreviousEntry(childName0).getName());
    assertEquals(childName1, branchLayout.findPreviousEntry(parentName1).getName());
    assertNull(branchLayout.findPreviousEntry(rootName));
    assertNull(branchLayout.findNextEntry(childName2));
  }

  @Test
  public void givenSingleEntryLayout_nextAndPreviousShouldBeNull() {
    // given
    String rootName = "root";

    val rootEntry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, List.empty());
    val branchLayout = new BranchLayout(List.of(rootEntry));
    //then
    assertNull(branchLayout.findPreviousEntry(rootName));
    assertNull(branchLayout.findNextEntry(rootName));
  }

  @Test
  public void givenOnlyRootsEntryLayout_nextAndPreviousShouldBeCalculated() {
    // given
    String rootName = "root";
    String rootName1 = "root1";
    String rootName2 = "root2";

    val rootEntry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, List.empty());
    val rootEntry1 = new BranchLayoutEntry(rootName1, /* customAnnotation */ null, List.empty());
    val rootEntry2 = new BranchLayoutEntry(rootName2, /* customAnnotation */ null, List.empty());
    val branchLayout = new BranchLayout(List.of(rootEntry, rootEntry1, rootEntry2));

    //then
    assertNull(branchLayout.findPreviousEntry(rootName));
    assertNull(branchLayout.findNextEntry(rootName2));
    assertEquals(rootName2, branchLayout.findNextEntry(rootName1).getName());
    assertEquals(rootName, branchLayout.findPreviousEntry(rootName1).getName());
    assertEquals(rootName1, branchLayout.findPreviousEntry(rootName2).getName());
  }

  @Test
  public void withBranchSlideOut_givenNonRootExistingBranch_slidesOut() {
    // given
    String rootName = "root";
    String branchToSlideOutName = "parent";
    String childName0 = "child0";
    String childName1 = "child1";

    /*-
        root                          root
          parent       slide out
            child0      ----->          child0
            child1                      child1
    */

    List<BranchLayoutEntry> childBranches = List.of(
        new BranchLayoutEntry(childName0, /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry(childName1, /* customAnnotation */ null, List.empty()));

    val entry = new BranchLayoutEntry(branchToSlideOutName, /* customAnnotation */ null, childBranches);
    val rootEntry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, List.of(entry));
    val branchLayout = new BranchLayout(List.of(rootEntry));

    assertNull(rootEntry.getParent());
    assertEquals(rootName, rootEntry.getChildren().get(0).getParent().getName());
    assertEquals(branchToSlideOutName, rootEntry.getChildren().get(0).getChildren().get(0).getParent().getName());
    assertEquals(branchToSlideOutName, rootEntry.getChildren().get(0).getChildren().get(1).getParent().getName());

    // when
    BranchLayout result = branchLayout.slideOut(branchToSlideOutName);

    // then
    assertEquals(1, result.getRootEntries().size());
    assertEquals(rootName, result.getRootEntries().get(0).getName());
    val children = result.getRootEntries().get(0).getChildren();
    assertEquals(2, children.size());
    assertEquals(childName0, children.get(0).getName());
    assertEquals(childName1, children.get(1).getName());

    assertNull(rootEntry.getParent());
    assertEquals(rootName, children.get(0).getParent().getName());
    assertEquals(rootName, children.get(1).getParent().getName());
  }

  @Test
  public void withBranchRename_givenBranchLayout_renamesChild() {
    // given
    String rootName = "root";
    String branchToRename = "child";
    String newBranchName = "kinder";

    /*-
        root         rename         root
          child      ----->           kinder
    */

    List<BranchLayoutEntry> childBranches = List.of(
        new BranchLayoutEntry(branchToRename, /* customAnnotation */ null, List.empty()));

    val rootEntry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, childBranches);
    val branchLayout = new BranchLayout(List.of(rootEntry));

    // when
    BranchLayout result = branchLayout.rename(branchToRename, newBranchName);

    // then
    assertEquals(1, result.getRootEntries().size());
    assertEquals(rootName, result.getRootEntries().get(0).getName());
    val children = result.getRootEntries().get(0).getChildren();
    assertEquals(1, children.size());
    assertEquals(newBranchName, children.get(0).getName());
  }

  @Test
  public void withBranchRename_givenBranchLayout_renamesRoot() {
    // given
    String rootName = "root";
    String rootAnnotation = "this is root";
    String child = "child";
    String newRootName = "master";

    /*-
        root         rename         master
          child      ----->           child
    */

    List<BranchLayoutEntry> childBranches = List.of(
        new BranchLayoutEntry(child, /* customAnnotation */ null, List.empty()));

    val rootEntry = new BranchLayoutEntry(rootName, rootAnnotation, childBranches);
    val branchLayout = new BranchLayout(List.of(rootEntry));

    // when
    BranchLayout result = branchLayout.rename(rootName, newRootName);

    // then
    assertEquals(1, result.getRootEntries().size());
    assertEquals(newRootName, result.getRootEntries().get(0).getName());
    assertEquals(rootAnnotation, result.getRootEntries().get(0).getCustomAnnotation());
    val children = result.getRootEntries().get(0).getChildren();
    assertEquals(1, children.size());
  }

  @Test
  public void withBranchRename_givenBranchLayout_renamesToTheSameName() {
    // given
    String rootName = "root";
    String child = "child";

    /*-
        root         rename         root
          child      ----->           child
    */

    List<BranchLayoutEntry> childBranches = List.of(
        new BranchLayoutEntry(child, /* customAnnotation */ null, List.empty()));

    val rootEntry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, childBranches);
    val branchLayout = new BranchLayout(List.of(rootEntry));

    // when
    BranchLayout result = branchLayout.rename(rootName, rootName);

    // then
    assertEquals(1, result.getRootEntries().size());
    assertEquals(rootName, result.getRootEntries().get(0).getName());
    val children = result.getRootEntries().get(0).getChildren();
    assertEquals(1, children.size());
  }

  @Test
  public void withBranchRename_givenBranchLayout_renameOfNonexistentDoesNothing() {
    // given
    String rootName = "root";
    String child = "child";
    String nonExisting = "fix/foo";
    String newNonExistingName = "bugfix/bar";

    /*-
        root         rename         root
          child      ----->           child
    */

    List<BranchLayoutEntry> childBranches = List.of(
        new BranchLayoutEntry(child, /* customAnnotation */ null, List.empty()));

    val rootEntry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, childBranches);
    val branchLayout = new BranchLayout(List.of(rootEntry));

    // when
    BranchLayout result = branchLayout.rename(nonExisting, newNonExistingName);

    // then
    assertEquals(1, result.getRootEntries().size());
    assertEquals(rootName, result.getRootEntries().get(0).getName());
    val children = result.getRootEntries().get(0).getChildren();
    assertEquals(1, children.size());
  }

  @Test
  public void withBranchSlideOut_givenDuplicatedBranch_slidesOut() {
    // given
    String rootName = "root";
    String branchToSlideOutName = "child";

    /*-
        root                        root
          child      slide out
          child       ----->
    */

    List<BranchLayoutEntry> childBranches = List.of(
        new BranchLayoutEntry(branchToSlideOutName, /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry(branchToSlideOutName, /* customAnnotation */ null, List.empty()));

    val rootEntry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, childBranches);
    val branchLayout = new BranchLayout(List.of(rootEntry));

    // when
    BranchLayout result = branchLayout.slideOut(branchToSlideOutName);

    // then
    assertEquals(1, result.getRootEntries().size());
    assertEquals(rootName, result.getRootEntries().get(0).getName());
    val children = result.getRootEntries().get(0).getChildren();
    assertEquals(0, children.size());
  }

  @Test
  public void withBranchSlideOut_givenRootBranchWithChildren_slidesOut() {
    // given
    String rootName = "root";
    String childName0 = "child0";
    String childName1 = "child1";

    /*-
            root          slide out
              child0       ----->        child0
              child1                     child1
    */

    List<BranchLayoutEntry> childBranches = List.of(
        new BranchLayoutEntry(childName0, /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry(childName1, /* customAnnotation */ null, List.empty()));

    val entry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, childBranches);
    val branchLayout = new BranchLayout(List.of(entry));

    // when
    BranchLayout result = branchLayout.slideOut(rootName);

    // then
    assertEquals(2, result.getRootEntries().size());
    assertEquals(childName0, result.getRootEntries().get(0).getName());
    assertEquals(childName1, result.getRootEntries().get(1).getName());
  }

  @Test
  public void withBranchSlideOut_givenDuplicatedBranchUnderItself_slidesOut() {
    // given
    String rootName = "root";
    String childName = "child";

    /*-
            root           slide out      root
              child         ----->
                child
    */

    val childBranchEntry = new BranchLayoutEntry(childName, /* customAnnotation */ null, List.empty());
    List<BranchLayoutEntry> childBranches = List.of(
        new BranchLayoutEntry(childName, /* customAnnotation */ null, List.of(childBranchEntry)));

    val entry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, childBranches);
    val branchLayout = new BranchLayout(List.of(entry));

    // when
    BranchLayout result = branchLayout.slideOut(childName);

    // then
    assertEquals(1, result.getRootEntries().size());
    assertEquals(rootName, result.getRootEntries().get(0).getName());
    assertEquals(0, result.getRootEntries().get(0).getChildren().size());
  }

  @Test
  public void withBranchSlideOut_givenSingleRootBranch_slidesOut() {
    // given
    val rootName = "root";

    /*-
            root       slide out
                        ----->
    */

    val entry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, List.empty());
    val branchLayout = new BranchLayout(List.of(entry));

    // when
    BranchLayout result = branchLayout.slideOut(rootName);

    // then
    assertEquals(0, result.getRootEntries().size());
  }

  @Test
  public void withBranchSlideOut_givenTwoRootBranches_slidesOut() {
    // given
    val rootName = "root";
    val masterRootName = "master";

    /*-
            root        slide out      master
            master       ----->
    */

    val entry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, List.empty());
    val masterEntry = new BranchLayoutEntry(masterRootName, /* customAnnotation */ null, List.empty());
    val branchLayout = new BranchLayout(List.of(entry, masterEntry));

    // when
    BranchLayout result = branchLayout.slideOut(rootName);

    // then
    assertEquals(1, result.getRootEntries().size());
    assertEquals(masterRootName, result.getRootEntries().get(0).getName());
  }

  @Test
  public void withBranchSlideOut_givenNonExistingBranch_noExceptionThrown() {
    // given
    val branchToSlideOutName = "branch";
    val branchLayout = new BranchLayout(List.empty());

    // when
    BranchLayout result = branchLayout.slideOut(branchToSlideOutName);

    // then no exception thrown
    assertTrue(result.getRootEntries().isEmpty());
  }

  private static BranchLayout getExampleBranchLayout() {
    return new BranchLayout(List.of(
        new BranchLayoutEntry("A", /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry("B", /* customAnnotation */ null,
            List.of(new BranchLayoutEntry("BA", /* customAnnotation */ null, List.empty())))));
  }

  @Test
  public void withBranchLayouts_givenTheyAreEquivalent_shouldBeConsideredEqual() {
    assertEquals(getExampleBranchLayout(), getExampleBranchLayout());
  }

  @Test
  public void withBranchLayouts_givenOneIsEmpty_shouldBeConsideredNotEqual() {
    val emptyBranchLayout = new BranchLayout(List.empty());
    val branchLayout = getExampleBranchLayout();

    assertNotEquals(branchLayout, emptyBranchLayout);
    assertNotEquals(emptyBranchLayout, branchLayout);
  }

  @Test
  public void withBranchLayouts_givenOneHasExtraChildBranch_shouldBeConsideredNotEqual() {
    val branchLayout = getExampleBranchLayout();
    val branchLayoutWithExtraChildBranch = new BranchLayout(List.of(
        new BranchLayoutEntry("A", /* customAnnotation */ null,
            List.of(new BranchLayoutEntry("EXTRA_BRANCH", /* customAnnotation */ null, List.empty()))),
        new BranchLayoutEntry("B", /* customAnnotation */ null,
            List.of(new BranchLayoutEntry("BA", /* customAnnotation */ null, List.empty())))));

    assertNotEquals(branchLayout, branchLayoutWithExtraChildBranch);
    assertNotEquals(branchLayoutWithExtraChildBranch, branchLayout);
  }

  @Test
  public void withBranchLayouts_givenOneHasExtraRootBranch_shouldBeConsideredNotEqual() {
    val branchLayout = getExampleBranchLayout();
    val branchLayoutWithExtraRootBranch = new BranchLayout(List.of(
        new BranchLayoutEntry("A", /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry("B", /* customAnnotation */ null,
            List.of(new BranchLayoutEntry("BA", /* customAnnotation */ null, List.empty()))),
        new BranchLayoutEntry("EXTRA_BRANCH", /* customAnnotation */ null, List.empty())));

    assertNotEquals(branchLayout, branchLayoutWithExtraRootBranch);
    assertNotEquals(branchLayoutWithExtraRootBranch, branchLayout);
  }

  @Test
  public void withBranchLayouts_givenOneHasDifferentRootBranchName_shouldBeConsideredNotEqual() {
    val branchLayout = getExampleBranchLayout();
    val branchLayoutDifferentRootName = new BranchLayout(List.of(
        new BranchLayoutEntry("DIFFERENT_NAME", /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry("B", /* customAnnotation */ null,
            List.of(new BranchLayoutEntry("BA", /* customAnnotation */ null, List.empty())))));

    assertNotEquals(branchLayout, branchLayoutDifferentRootName);
    assertNotEquals(branchLayoutDifferentRootName, branchLayout);
  }

  @Test
  public void withBranchLayouts_givenOneHasDifferentChildBranchName_shouldBeConsideredNotEqual() {
    val branchLayout = getExampleBranchLayout();
    val branchLayoutDifferentLeafName = new BranchLayout(List.of(
        new BranchLayoutEntry("A", /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry("B", /* customAnnotation */ null,
            List.of(new BranchLayoutEntry("DIFFERENT_NAME", /* customAnnotation */ null, List.empty())))));

    assertNotEquals(branchLayout, branchLayoutDifferentLeafName);
    assertNotEquals(branchLayoutDifferentLeafName, branchLayout);
  }

  @Test
  public void withBranchLayouts_givenOneHasDifferentChildBranchCustomAnnotation_shouldBeConsideredNotEqual() {
    val branchLayout = getExampleBranchLayout();
    val branchLayoutDifferentAnnotation = new BranchLayout(List.of(
        new BranchLayoutEntry("A", /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry("B", "DIFFERENT_ANNOTATION",
            List.of(new BranchLayoutEntry("BA", /* customAnnotation */ null, List.empty())))));

    assertNotEquals(branchLayout, branchLayoutDifferentAnnotation);
    assertNotEquals(branchLayoutDifferentAnnotation, branchLayout);
  }

  @Test
  public void withBranchLayouts_givenTheyAreEquivalentButShuffled_shouldBeConsideredEqual() {
    val branchLayout = new BranchLayout(List.of(
        new BranchLayoutEntry("A", /* customAnnotation */ null,
            List.of(new BranchLayoutEntry("AB", "ANNOTATION", List.empty()),
                new BranchLayoutEntry("AA", /* customAnnotation */ null, List.empty()))),
        new BranchLayoutEntry("B", /* customAnnotation */ null,
            List.of(new BranchLayoutEntry("BA", /* customAnnotation */ null, List.empty())))));

    val branchLayoutShuffled = new BranchLayout(List.of(
        new BranchLayoutEntry("B", /* customAnnotation */ null,
            List.of(new BranchLayoutEntry("BA", /* customAnnotation */ null, List.empty()))),
        new BranchLayoutEntry("A", /* customAnnotation */ null,
            List.of(new BranchLayoutEntry("AA", /* customAnnotation */ null, List.empty()),
                new BranchLayoutEntry("AB", "ANNOTATION", List.empty())))));

    assertEquals(branchLayout, branchLayoutShuffled);
    assertEquals(branchLayoutShuffled, branchLayout);
  }
}
