package com.virtuslab.branchlayout.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.vavr.collection.List;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;

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
    assertEquals(branchLayout.findNextEntry(childName1).getName(), parentName1);
    assertEquals(branchLayout.findPreviousEntry(childName1).getName(), childName0);
    assertEquals(branchLayout.findNextEntry(parentName0).getName(), childName0);
    assertEquals(branchLayout.findPreviousEntry(childName0).getName(), parentName0);
    assertEquals(branchLayout.findPreviousEntry(parentName1).getName(), childName1);
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
    assertEquals(branchLayout.findNextEntry(rootName1).getName(), rootName2);
    assertEquals(branchLayout.findPreviousEntry(rootName1).getName(), rootName);
    assertEquals(branchLayout.findPreviousEntry(rootName2).getName(), rootName1);
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
    assertEquals(children.size(), 2);
    assertEquals(childName0, children.get(0).getName());
    assertEquals(childName1, children.get(1).getName());

    assertNull(rootEntry.getParent());
    assertEquals(rootName, children.get(0).getParent().getName());
    assertEquals(rootName, children.get(1).getParent().getName());
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
    assertEquals(result.getRootEntries().size(), 1);
    assertEquals(result.getRootEntries().get(0).getName(), rootName);
    val children = result.getRootEntries().get(0).getChildren();
    assertEquals(children.size(), 0);
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
    assertEquals(result.getRootEntries().size(), 2);
    assertEquals(result.getRootEntries().get(0).getName(), childName0);
    assertEquals(result.getRootEntries().get(1).getName(), childName1);
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
    assertEquals(result.getRootEntries().size(), 1);
    assertEquals(result.getRootEntries().get(0).getName(), rootName);
    assertEquals(result.getRootEntries().get(0).getChildren().size(), 0);
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
    assertEquals(result.getRootEntries().size(), 0);
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
    assertEquals(result.getRootEntries().size(), 1);
    assertEquals(result.getRootEntries().get(0).getName(), masterRootName);
  }

  @Test
  public void withBranchSlideOut_givenNonExistingBranch_noExceptionThrown() {
    // given
    val branchToSlideOutName = "branch";
    val branchLayout = new BranchLayout(List.empty());

    // when
    BranchLayout result = branchLayout.slideOut(branchToSlideOutName);

    // then no exception thrown
    Assert.assertTrue(result.getRootEntries().isEmpty());
  }

  private static BranchLayout getExampleBranchLayout() {
    return new BranchLayout(List.of(
        new BranchLayoutEntry("A", /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry("B", /* customAnnotation */ null,
            List.of(new BranchLayoutEntry("BA", /* customAnnotation */ null, List.empty())))));
  }

  @Test
  public void withBranchLayouts_givenTheyAreEquivalent_shouldBeConsideredEqual() {
    Assert.assertTrue(getExampleBranchLayout().equals(getExampleBranchLayout()));
  }

  @Test
  public void withBranchLayouts_givenOneIsEmpty_shouldBeConsideredNotEqual() {
    val emptyBranchLayout = new BranchLayout(List.empty());
    val branchLayout = getExampleBranchLayout();

    Assert.assertFalse(branchLayout.equals(emptyBranchLayout));
    Assert.assertFalse(emptyBranchLayout.equals(branchLayout));
  }

  @Test
  public void withBranchLayouts_givenOneHasExtraChildBranch_shouldBeConsideredNotEqual() {
    val branchLayout = getExampleBranchLayout();
    val branchLayoutWithExtraChildBranch = new BranchLayout(List.of(
        new BranchLayoutEntry("A", /* customAnnotation */ null,
            List.of(new BranchLayoutEntry("EXTRA_BRANCH", /* customAnnotation */ null, List.empty()))),
        new BranchLayoutEntry("B", /* customAnnotation */ null,
            List.of(new BranchLayoutEntry("BA", /* customAnnotation */ null, List.empty())))));

    Assert.assertFalse(branchLayout.equals(branchLayoutWithExtraChildBranch));
    Assert.assertFalse(branchLayoutWithExtraChildBranch.equals(branchLayout));
  }

  @Test
  public void withBranchLayouts_givenOneHasExtraRootBranch_shouldBeConsideredNotEqual() {
    val branchLayout = getExampleBranchLayout();
    val branchLayoutWithExtraRootBranch = new BranchLayout(List.of(
        new BranchLayoutEntry("A", /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry("B", /* customAnnotation */ null,
            List.of(new BranchLayoutEntry("BA", /* customAnnotation */ null, List.empty()))),
        new BranchLayoutEntry("EXTRA_BRANCH", /* customAnnotation */ null, List.empty())));

    Assert.assertFalse(branchLayout.equals(branchLayoutWithExtraRootBranch));
    Assert.assertFalse(branchLayoutWithExtraRootBranch.equals(branchLayout));
  }

  @Test
  public void withBranchLayouts_givenOneHasDifferentRootBranchName_shouldBeConsideredNotEqual() {
    val branchLayout = getExampleBranchLayout();
    val branchLayoutDifferentRootName = new BranchLayout(List.of(
        new BranchLayoutEntry("DIFFERENT_NAME", /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry("B", /* customAnnotation */ null,
            List.of(new BranchLayoutEntry("BA", /* customAnnotation */ null, List.empty())))));

    Assert.assertFalse(branchLayout.equals(branchLayoutDifferentRootName));
    Assert.assertFalse(branchLayoutDifferentRootName.equals(branchLayout));
  }

  @Test
  public void withBranchLayouts_givenOneHasDifferentChildBranchName_shouldBeConsideredNotEqual() {
    val branchLayout = getExampleBranchLayout();
    val branchLayoutDifferentLeafName = new BranchLayout(List.of(
        new BranchLayoutEntry("A", /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry("B", /* customAnnotation */ null,
            List.of(new BranchLayoutEntry("DIFFERENT_NAME", /* customAnnotation */ null, List.empty())))));

    Assert.assertFalse(branchLayout.equals(branchLayoutDifferentLeafName));
    Assert.assertFalse(branchLayoutDifferentLeafName.equals(branchLayout));
  }

  @Test
  public void withBranchLayouts_givenOneHasDifferentChildBranchCustomAnnotation_shouldBeConsideredNotEqual() {
    val branchLayout = getExampleBranchLayout();
    val branchLayoutDifferentAnnotation = new BranchLayout(List.of(
        new BranchLayoutEntry("A", /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry("B", "DIFFERENT_ANNOTATION",
            List.of(new BranchLayoutEntry("BA", /* customAnnotation */ null, List.empty())))));

    Assert.assertFalse(branchLayout.equals(branchLayoutDifferentAnnotation));
    Assert.assertFalse(branchLayoutDifferentAnnotation.equals(branchLayout));
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

    Assert.assertTrue(branchLayout.equals(branchLayoutShuffled));
    Assert.assertTrue(branchLayoutShuffled.equals(branchLayout));
  }
}
