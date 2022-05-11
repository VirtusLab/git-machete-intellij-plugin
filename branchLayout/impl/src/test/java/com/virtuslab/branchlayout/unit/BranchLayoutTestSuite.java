package com.virtuslab.branchlayout.unit;

import static org.junit.Assert.assertEquals;

import io.vavr.collection.List;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;

public class BranchLayoutTestSuite {

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

    List<IBranchLayoutEntry> childBranches = List.of(
        new BranchLayoutEntry(childName0, /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry(childName1, /* customAnnotation */ null, List.empty()));

    val entry = new BranchLayoutEntry(branchToSlideOutName, /* customAnnotation */ null, childBranches);
    val rootEntry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, List.of(entry));
    val branchLayout = new BranchLayout(List.of(rootEntry));

    // when
    IBranchLayout result = branchLayout.slideOut(branchToSlideOutName);

    // then
    assertEquals(result.getRootEntries().size(), 1);
    assertEquals(result.getRootEntries().get(0).getName(), rootName);
    val children = result.getRootEntries().get(0).getChildren();
    assertEquals(children.size(), 2);
    assertEquals(children.get(0).getName(), childName0);
    assertEquals(children.get(1).getName(), childName1);
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

    List<IBranchLayoutEntry> childBranches = List.of(
        new BranchLayoutEntry(branchToSlideOutName, /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry(branchToSlideOutName, /* customAnnotation */ null, List.empty()));

    val rootEntry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, childBranches);
    val branchLayout = new BranchLayout(List.of(rootEntry));

    // when
    IBranchLayout result = branchLayout.slideOut(branchToSlideOutName);

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

    List<IBranchLayoutEntry> childBranches = List.of(
        new BranchLayoutEntry(childName0, /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry(childName1, /* customAnnotation */ null, List.empty()));

    val entry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, childBranches);
    val branchLayout = new BranchLayout(List.of(entry));

    // when
    IBranchLayout result = branchLayout.slideOut(rootName);

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
    List<IBranchLayoutEntry> childBranches = List.of(
        new BranchLayoutEntry(childName, /* customAnnotation */ null, List.of(childBranchEntry)));

    val entry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, childBranches);
    val branchLayout = new BranchLayout(List.of(entry));

    // when
    IBranchLayout result = branchLayout.slideOut(childName);

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
    IBranchLayout result = branchLayout.slideOut(rootName);

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
    IBranchLayout result = branchLayout.slideOut(rootName);

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
    IBranchLayout result = branchLayout.slideOut(branchToSlideOutName);

    // then no exception thrown
    Assert.assertTrue(result.getRootEntries().isEmpty());
  }
}
