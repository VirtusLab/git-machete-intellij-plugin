package com.virtuslab.branchlayout.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.impl.readwrite.BranchLayoutFileReader;
import com.virtuslab.branchlayout.impl.readwrite.BranchLayoutFileUtils;
import com.virtuslab.branchlayout.impl.readwrite.IndentSpec;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BranchLayoutFileUtils.class, Files.class})
public class BranchLayoutFileReaderTestSuite {

  private final Path path = Paths.get("");

  @SneakyThrows
  private BranchLayoutFileReader getBranchLayoutFileReaderForLines(List<String> linesToReturn, int indentWidth) {
    BranchLayoutFileReader reader = new BranchLayoutFileReader();

    PowerMockito.mockStatic(BranchLayoutFileUtils.class);
    val indentSpec = new IndentSpec(/* indentCharacter */ IndentSpec.SPACE, indentWidth);
    PowerMockito.when(BranchLayoutFileUtils.getDefaultSpec()).thenReturn(indentSpec);
    PowerMockito.when(BranchLayoutFileUtils.readFileLines(any())).thenReturn(linesToReturn);
    PowerMockito.when(BranchLayoutFileUtils.getIndentWidth(anyString(), anyChar())).thenCallRealMethod();
    PowerMockito.when(BranchLayoutFileUtils.hasProperIndentationCharacter(anyString(), anyChar())).thenCallRealMethod();

    PowerMockito.mockStatic(Files.class);
    PowerMockito.when(Files.isRegularFile(any())).thenReturn(false);

    return reader;
  }

  @Test
  @SneakyThrows
  public void read_givenCorrectFile_reads() {
    // given
    List<String> linesToReturn = List.of(" ", "A", " B", "C", "");
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn, /* indentWidth */ 1);

    // when
    BranchLayout branchLayout = reader.read(path);

    // then
    Assert.assertNotNull(branchLayout.findEntryByName("A"));
    Assert.assertNotNull(branchLayout.findEntryByName("B"));
    Assert.assertNotNull(branchLayout.findEntryByName("C"));
    Assert.assertEquals(2, branchLayout.getRootEntries().size());
  }

  @Test
  @SneakyThrows
  public void read_givenCorrectFileWithRootsOnly_reads() {
    // given
    List<String> linesToReturn = List.of("A", " ", "B");
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn, /* indentWidth */ 1);

    // when
    BranchLayout branchLayout = reader.read(path);

    // then
    Assert.assertNotNull(branchLayout.findEntryByName("A"));
    Assert.assertNotNull(branchLayout.findEntryByName("B"));
    Assert.assertEquals(2, branchLayout.getRootEntries().size());
  }

  @Test
  @SneakyThrows
  public void read_givenEmptyFile_reads() {
    // given
    List<String> linesToReturn = List.empty();
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn, /* indentWidth */ 1);

    // when
    BranchLayout branchLayout = reader.read(path);

    // then no exception thrown
    Assert.assertEquals(0, branchLayout.getRootEntries().size());
  }

  @Test
  public void read_givenFileWithIndentedFirstEntry_throwsException() {
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
  public void read_givenFileWithIndentWidthNotAMultiplicityOfLevelWidth_throwsException() {
    // given
    List<String> linesToReturn = List.of("A", " ", "   B", " C");
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn, /* indentWidth */ 3);

    // when
    BranchLayoutException exception = assertThrows(BranchLayoutException.class, () -> reader.read(path));

    System.out.println(exception.getMessage());
    // then
    int i = exception.getErrorLine().get();
    assertEquals(4, i);
  }

  @Test
  public void read_givenFileWithChildIndentGreaterThanOneToParent_throwsException() {
    // given
    List<String> linesToReturn = List.of(" ", "A", "", "  B", "      C");
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn, /* indentWidth */ 2);

    // when
    BranchLayoutException exception = assertThrows(BranchLayoutException.class, () -> reader.read(path));

    // then
    int i = exception.getErrorLine().get();
    assertEquals(5, i);
  }

  @Test
  public void read_givenFileWithDifferentIndentCharacters_throwsException() {
    // given
    List<String> linesToReturn = List.of("A", " B", "\tC");
    BranchLayoutFileReader reader = getBranchLayoutFileReaderForLines(linesToReturn, /* indentWidth */ 1);

    // when
    BranchLayoutException exception = assertThrows(BranchLayoutException.class, () -> reader.read(path));

    // then
    int i = exception.getErrorLine().get();
    assertEquals(3, i);
  }
}
