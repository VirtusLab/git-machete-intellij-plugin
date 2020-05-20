package com.virtuslab.branchlayout.unit;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;

import io.vavr.collection.List;
import org.junit.Test;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;
import com.virtuslab.branchlayout.impl.BranchLayout;
import com.virtuslab.branchlayout.impl.BranchLayoutEntry;
import com.virtuslab.branchlayout.impl.IndentSpec;
import com.virtuslab.branchlayout.impl.readwrite.BranchLayoutFileUtils;

public class BranchLayoutTest {

  private final Path path = Path.of("");
  private final IndentSpec indentSpec = BranchLayoutFileUtils.getDefaultSpec();

  @Test(expected = BranchLayoutException.class)
  public void withBranchSlideOut_givenRootBranch_throwsException() throws BranchLayoutException {
    // given
    var entryToSlideOutName = "root";
    IBranchLayoutEntry entry = new BranchLayoutEntry(entryToSlideOutName, /* customAnnotation */ null, List.empty());
    var branchLayoutFile = new BranchLayout(List.of(entry), path, indentSpec);

    // when
    branchLayoutFile.slideOut(entryToSlideOutName);

    // then exception is thrown
  }

  @Test(expected = BranchLayoutException.class)
  public void withBranchSlideOut_givenNonExistingBranch_throwsException() throws BranchLayoutException {
    // given
    var branchToSlideOutName = "branch";
    var branchLayoutFile = new BranchLayout(List.empty(), path, indentSpec);

    // when
    branchLayoutFile.slideOut(branchToSlideOutName);

    // then exception is thrown
  }

  @Test
  public void withBranchSlideOut_givenNonRootExistingBranch_slidesOut() throws BranchLayoutException {
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

    var entry = new BranchLayoutEntry(branchToSlideOutName, /* customAnnotation */ null, childBranches);
    var rootEntry = new BranchLayoutEntry(rootName, /* customAnnotation */ null, List.of(entry));
    var branchLayoutFile = new BranchLayout(List.of(rootEntry), path, indentSpec);

    // when
    IBranchLayout result = branchLayoutFile.slideOut(branchToSlideOutName);

    // then
    assertEquals(result.getRootEntries().size(), 1);
    assertEquals(result.getRootEntries().get(0).getName(), rootName);
    var subentries = result.getRootEntries().get(0).getSubentries();
    assertEquals(subentries.size(), 2);
    assertEquals(subentries.get(0).getName(), childName0);
    assertEquals(subentries.get(1).getName(), childName1);
  }
}
