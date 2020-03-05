package com.virtuslab.branchrelationfile;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.virtuslab.branchrelationfile.api.BranchRelationFileException;
import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import com.virtuslab.branchrelationfile.impl.BranchRelationFile;
import com.virtuslab.branchrelationfile.impl.BranchRelationFileEntry;

@RunWith(PowerMockRunner.class)
@PrepareForTest(BranchRelationFile.class)
public class BranchRelationFileTest {

  BranchRelationFile branchRelationFile;

  @Before
  public void init() {
    branchRelationFile = Whitebox.newInstance(BranchRelationFile.class);
    Whitebox.setInternalState(branchRelationFile, "rootBranches", new LinkedList<>());
    PowerMockito.suppress(PowerMockito.method(BranchRelationFile.class, "saveToFile"));
  }

  @Test(expected = BranchRelationFileException.class)
  public void withBranchSlidOut_givenRootBranch_throwsException() throws BranchRelationFileException, IOException {
    // given
    var branchToSlideOutEntry = new BranchRelationFileEntry("root", null, null);

    // when
    branchRelationFile.withBranchSlidOut(branchToSlideOutEntry);

    // then exception is thrown
  }

  @Test(expected = BranchRelationFileException.class)
  public void withBranchSlidOut_givenNonExistingBranch_throwsException()
      throws BranchRelationFileException, IOException {
    // given
    var branchToSlideOutName = "branch";

    // when
    branchRelationFile.withBranchSlidOut(branchToSlideOutName);

    // then exception is thrown
  }

  @Test
  public void withBranchSlidOut_givenNonRootExistingBranch_slidesOut() throws BranchRelationFileException, IOException {
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

    var rootEntry = new BranchRelationFileEntry(rootName, null, null);
    var entry = new BranchRelationFileEntry(branchToSlideOutName, rootEntry, null);
    rootEntry.addSubbranch(entry);
    entry.getSubbranches().addAll(List.of(new BranchRelationFileEntry(childName0, entry, null),
        new BranchRelationFileEntry(childName1, entry, null)));
    branchRelationFile.getRootBranches().add(rootEntry);

    // when
    IBranchRelationFile result = branchRelationFile.withBranchSlidOut(branchToSlideOutName);

    // then
    assertEquals(result.getRootBranches().size(), 1);
    assertEquals(result.getRootBranches().get(0).getName(), rootName);
    var subbranches = result.getRootBranches().get(0).getSubbranches();
    assertEquals(subbranches.size(), 2);
    assertEquals(subbranches.get(0).getName(), childName0);
    assertEquals(subbranches.get(1).getName(), childName1);
  }
}
