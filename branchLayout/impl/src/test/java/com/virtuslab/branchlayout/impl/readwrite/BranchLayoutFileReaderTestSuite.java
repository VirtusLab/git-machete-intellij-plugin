package com.virtuslab.branchlayout.impl.readwrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.stream.Collectors;

import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutException;

public class BranchLayoutFileReaderTestSuite {

  public static InputStream getInputStreamFromLines(List<String> lines) {
    return new ByteArrayInputStream(
        lines.collect(Collectors.joining(System.lineSeparator()))
            .getBytes());
  }

  @Test
  @SneakyThrows
  public void read_givenCorrectFile_reads() {
    // given
    val linesToReturn = List.of(" ", "A", " B", "C", "");
    val linesStream = getInputStreamFromLines(linesToReturn);

    // when
    BranchLayout branchLayout = new BranchLayoutReader().read(linesStream);

    // then
    assertNotNull(branchLayout.getEntryByName("A"));
    assertNotNull(branchLayout.getEntryByName("B"));
    assertNotNull(branchLayout.getEntryByName("C"));
    assertEquals(2, branchLayout.getRootEntries().size());

    val a = branchLayout.getRootEntries().get(0);
    val b = a.getChildren().get(0);
    val c = branchLayout.getRootEntries().get(0);
    assertSame(a, b.getParent());
    assertNull(a.getParent());
    assertNull(c.getParent());
  }

  @Test
  @SneakyThrows
  public void read_givenCorrectFileWithRootsOnly_reads() {
    // given
    List<String> linesToReturn = List.of("A", " ", "B");
    val linesStream = getInputStreamFromLines(linesToReturn);

    // when
    BranchLayout branchLayout = new BranchLayoutReader().read(linesStream);

    // then
    assertNotNull(branchLayout.getEntryByName("A"));
    assertNotNull(branchLayout.getEntryByName("B"));
    assertEquals(2, branchLayout.getRootEntries().size());
  }

  @Test
  @SneakyThrows
  public void read_givenEmptyFile_reads() {
    // given
    List<String> linesToReturn = List.empty();
    val linesStream = getInputStreamFromLines(linesToReturn);

    // when
    BranchLayout branchLayout = new BranchLayoutReader().read(linesStream);

    // then no exception thrown
    assertEquals(0, branchLayout.getRootEntries().size());
  }

  @Test
  public void read_givenFileWithIndentedFirstEntry_throwsException() {
    // given
    List<String> linesToReturn = List.of(" ", " ", " A", " B", " ");
    val linesStream = getInputStreamFromLines(linesToReturn);

    // when
    BranchLayoutException exception = assertThrows(BranchLayoutException.class,
        () -> new BranchLayoutReader().read(linesStream));

    // then
    int i = exception.getErrorLine();
    assertEquals(3, i);
  }

  @Test
  public void read_givenFileWithIndentWidthNotAMultiplicityOfLevelWidth_throwsException() {
    // given
    List<String> linesToReturn = List.of("A", " ", "   B", " C");
    val linesStream = getInputStreamFromLines(linesToReturn);

    // when
    BranchLayoutException exception = assertThrows(BranchLayoutException.class,
        () -> new BranchLayoutReader().read(linesStream));

    System.out.println(exception.getMessage());
    // then
    int i = exception.getErrorLine();
    assertEquals(4, i);
  }

  @Test
  public void read_givenFileWithChildIndentGreaterThanOneToParent_throwsException() {
    // given
    List<String> linesToReturn = List.of(" ", "A", "", "  B", "      C");
    val linesStream = getInputStreamFromLines(linesToReturn);

    // when
    BranchLayoutException exception = assertThrows(BranchLayoutException.class,
        () -> new BranchLayoutReader().read(linesStream));

    // then
    int i = exception.getErrorLine();
    assertEquals(5, i);
  }

  @Test
  public void read_givenFileWithDifferentIndentCharacters_throwsException() {
    // given
    List<String> linesToReturn = List.of("A", " B", "\tC");
    val linesStream = getInputStreamFromLines(linesToReturn);

    // when
    BranchLayoutException exception = assertThrows(BranchLayoutException.class,
        () -> new BranchLayoutReader().read(linesStream));

    // then
    int i = exception.getErrorLine();
    assertEquals(3, i);
  }
}
