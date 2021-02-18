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
        root                           root
            parent         slide out
                  child0    ----->         child0
                  child1                   child1
    */

    List<IBranchLayoutEntry> childBranches = List.of(
        new BranchLayoutEntry(childName0, /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry(childName1, /* customAnnotation */ null, List.empty()));

    val entry = new BranchLayoutEntry(branchToSlideOutName, /* customAnnotation */ null, childBranches);
    val rootEntry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, List.of(entry));
    val branchLayoutFile = new BranchLayout(List.of(rootEntry));

    // when
    IBranchLayout result = branchLayoutFile.slideOut(branchToSlideOutName);

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
        root                           root
            child       slide out
            child        ----->
    */

    List<IBranchLayoutEntry> childBranches = List.of(
        new BranchLayoutEntry(branchToSlideOutName, /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry(branchToSlideOutName, /* customAnnotation */ null, List.empty()));

    val rootEntry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, childBranches);
    val branchLayoutFile = new BranchLayout(List.of(rootEntry));

    // when
    IBranchLayout result = branchLayoutFile.slideOut(branchToSlideOutName);

    // then
    assertEquals(result.getRootEntries().size(), 1);
    assertEquals(result.getRootEntries().get(0).getName(), rootName);
    val children = result.getRootEntries().get(0).getChildren();
    assertEquals(children.size(), 0);
  }

  @Test
  public void withBranchSlideOut_givenRootBranchWithChildren_throwsException() {
    // given
    String rootName = "root";
    String childName0 = "child0";
    String childName1 = "child1";

    /*-
            root           slide out
                  child0    ----->     child0
                  child1               child1
    */

    List<IBranchLayoutEntry> childBranches = List.of(
        new BranchLayoutEntry(childName0, /* customAnnotation */ null, List.empty()),
        new BranchLayoutEntry(childName1, /* customAnnotation */ null, List.empty()));

    val entry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, childBranches);
    val branchLayoutFile = new BranchLayout(List.of(entry));

    // when
    IBranchLayout result = branchLayoutFile.slideOut(rootName);

    // then
    assertEquals(result.getRootEntries().size(), 2);
    assertEquals(result.getRootEntries().get(0).getName(), childName0);
    assertEquals(result.getRootEntries().get(1).getName(), childName1);
  }

  @Test
  public void withBranchSlideOut_givenSingleRootBranch_throwsException() {
    // given
    val rootName = "root";
    IBranchLayoutEntry entry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, List.empty());
    val branchLayoutFile = new BranchLayout(List.of(entry));

    // when
    val result = branchLayoutFile.slideOut(rootName);

    // then
    assertEquals(result.getRootEntries().size(), 0);
  }

  @Test
  public void withBranchSlideOut_givenTwoRootBranches_throwsException() {
    // given
    val rootName = "root";
    val masterRootName = "master";
    IBranchLayoutEntry entry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, List.empty());
    IBranchLayoutEntry masterEntry = new BranchLayoutEntry(masterRootName, /* customAnnotation */ null, List.empty());
    val branchLayoutFile = new BranchLayout(List.of(entry, masterEntry));

    // when
    val result = branchLayoutFile.slideOut(rootName);

    // then
    assertEquals(result.getRootEntries().size(), 1);
    assertEquals(result.getRootEntries().get(0).getName(), masterRootName);
  }

  @Test
  public void withBranchSlideOut_givenNonExistingBranch_noExceptionThrown() {
    // given
    val branchToSlideOutName = "branch";
    val branchLayoutFile = new BranchLayout(List.empty());

    // when
    branchLayoutFile.slideOut(branchToSlideOutName);

    // then no exception thrown
    Assert.assertTrue(branchLayoutFile.getRootEntries().isEmpty());
  }
}
