package com.virtuslab.branchlayout.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;

import java.nio.file.Files;
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
import com.virtuslab.branchlayout.impl.IndentSpec;
import com.virtuslab.branchlayout.impl.readwrite.BranchLayoutFileReader;
import com.virtuslab.branchlayout.impl.readwrite.BranchLayoutFileUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BranchLayoutFileUtils.class, Files.class})
public class BranchLayoutFileReaderTest {

  private final Path path = Path.of("");

  private BranchLayoutFileReader getBranchLayoutFileReaderForLines(List<String> linesToReturn, int indentWidth)
      throws Exception {
    BranchLayoutFileReader reader = new BranchLayoutFileReader();

    PowerMockito.mockStatic(BranchLayoutFileUtils.class);
    var indentSpec = new IndentSpec(/* indentCharacter */ ' ', indentWidth);
    PowerMockito.when(BranchLayoutFileUtils.getDefaultSpec()).thenReturn(indentSpec);
    PowerMockito.when(BranchLayoutFileUtils.readFileLines(any())).thenReturn(linesToReturn);
    PowerMockito.when(BranchLayoutFileUtils.getIndentWidth(anyString(), anyChar())).thenCallRealMethod();

    PowerMockito.mockStatic(Files.class);
    PowerMockito.when(Files.isRegularFile(any())).thenReturn(false);

    return reader;
  }

  @Test
  public void read_givenCorrectFile_reads() throws Exception {
    // given
    List<String> linesToReturn = List.of(" ", "A", " B", "C", "");
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn, /* indentWidth */ 1);

    // when
    BranchLayout branchLayout = reader.read(path);

    // then
    Assert.assertTrue(branchLayout.findEntryByName("A").isDefined());
    Assert.assertTrue(branchLayout.findEntryByName("B").isDefined());
    Assert.assertTrue(branchLayout.findEntryByName("C").isDefined());
    Assert.assertEquals(2, branchLayout.getRootEntries().size());
  }

  @Test
  public void read_givenCorrectFileWithRootsOnly_reads() throws Exception {
    // given
    List<String> linesToReturn = List.of("A", " ", "B");
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn, /* indentWidth */ 1);

    // when
    BranchLayout branchLayout = reader.read(path);

    // then
    Assert.assertTrue(branchLayout.findEntryByName("A").isDefined());
    Assert.assertTrue(branchLayout.findEntryByName("B").isDefined());
    Assert.assertEquals(2, branchLayout.getRootEntries().size());
  }

  @Test
  public void read_givenEmptyFile_reads() throws Exception {
    // given
    List<String> linesToReturn = List.empty();
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn, /* indentWidth */ 1);

    // when
    BranchLayout branchLayout = reader.read(path);

    // then no exception thrown
    Assert.assertEquals(0, branchLayout.getRootEntries().size());
  }

  @Test
  public void read_givenFileWithIndentedFirstEntry_throwsException() throws Exception {
    // given
    List<String> linesToReturn = List.of(" ", " ", " A", " B", " ");
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn, /* indentWidth */ 1);

    // when
    BranchLayoutException exception = assertThrows(BranchLayoutException.class, () -> reader.read(path));

    // then
    int i = exception.getErrorLine().get();
    assertEquals(3, i);
  }

  @Test
  public void read_givenFileWithIndentWidthNotAMultiplicityOfLevelWidth_throwsException() throws Exception {
    // given
    List<String> linesToReturn = List.of("A", " ", "   B", " C");
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn, /* indentWidth */ 3);

    // when
    BranchLayoutException exception = assertThrows(BranchLayoutException.class, () -> reader.read(path));

    System.out.println(exception);
    // then
    int i = exception.getErrorLine().get();
    assertEquals(4, i);
  }

  @Test
  public void read_givenFileWithSubentryIndentGreaterThanOneToParent_throwsException() throws Exception {
    // given
    List<String> linesToReturn = List.of(" ", "A", "", "  B", "      C");
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn, /* indentWidth */ 2);

    // when
    BranchLayoutException exception = assertThrows(BranchLayoutException.class, () -> reader.read(path));

    // then
    int i = exception.getErrorLine().get();
    assertEquals(5, i);
  }
}
