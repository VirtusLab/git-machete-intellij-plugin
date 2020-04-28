package com.virtuslab.branchlayout;

import static org.mockito.ArgumentMatchers.*;

import java.nio.file.Path;

import io.vavr.collection.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.impl.BranchLayout;
import com.virtuslab.branchlayout.impl.manager.BranchLayoutFileReader;
import com.virtuslab.branchlayout.impl.manager.BranchLayoutFileUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest(BranchLayoutFileUtils.class)
public class BranchLayoutFileReaderTest {

  private BranchLayoutFileReader getBranchLayoutFileReaderForLines(List<String> linesToReturn) throws Exception {
    BranchLayoutFileReader reader = new BranchLayoutFileReader(Path.of(""),
        /* indentCharacter */ ' ', /* indentWidth */1);
    PowerMockito.mockStatic(BranchLayoutFileUtils.class);
    PowerMockito.when(BranchLayoutFileUtils.getFileLines(any())).thenReturn(linesToReturn);
    PowerMockito.when(BranchLayoutFileUtils.getIndentIndentWidth(anyString(), anyChar())).thenCallRealMethod();
    return reader;
  }

  @Test
  public void read_givenCorrectFile_reads() throws Exception {
    // given
    List<String> linesToReturn = List.of("A", " B", "C");
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn);

    // when
    BranchLayout branchLayout = reader.read();

    // then
    Assert.assertTrue(branchLayout.findEntryByName("A").isDefined());
    Assert.assertTrue(branchLayout.findEntryByName("B").isDefined());
    Assert.assertTrue(branchLayout.findEntryByName("C").isDefined());
  }

  @Test
  public void read_givenCorrectFileWithRootsOnly_reads() throws Exception {
    // given
    List<String> linesToReturn = List.of("A", "B");
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn);

    // when
    BranchLayout branchLayout = reader.read();

    // then
    Assert.assertTrue(branchLayout.findEntryByName("A").isDefined());
    Assert.assertTrue(branchLayout.findEntryByName("B").isDefined());
  }

  @Test
  public void read_givenEmptyFile_reads() throws Exception {
    // given
    List<String> linesToReturn = List.empty();
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn);

    // when
    BranchLayout branchLayout = reader.read();

    // then no exception thrown
  }

  @Test(expected = BranchLayoutException.class)
  public void read_givenFileWithIndentedFirstEntry_throwsException() throws Exception {
    // given
    List<String> linesToReturn = List.of(" A", " B");
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn);

    // when
    BranchLayout branchLayout = reader.read();

    // then exception is thrown
  }

  @Test(expected = BranchLayoutException.class)
  public void read_givenFileWithIndentWidthNotAMultiplicityOfLevelWidth_throwsException() throws Exception {
    // given
    List<String> linesToReturn = List.of("A", "   B", " C");
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn);

    // when
    BranchLayout branchLayout = reader.read();

    // then exception is thrown
  }

  @Test(expected = BranchLayoutException.class)
  public void read_givenFileWithSubentryIndentGreaterThanOneToParent_throwsException() throws Exception {
    // given
    List<String> linesToReturn = List.of("A", "  B", "      C");
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn);

    // when
    BranchLayout branchLayout = reader.read();

    // then exception is thrown
  }
}
