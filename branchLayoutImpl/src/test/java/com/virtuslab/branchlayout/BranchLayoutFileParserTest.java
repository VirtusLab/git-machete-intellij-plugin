package com.virtuslab.branchlayout;

import static org.powermock.api.mockito.PowerMockito.doReturn;

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
import com.virtuslab.branchlayout.impl.BranchLayoutFileParser;

@RunWith(PowerMockRunner.class)
@PrepareForTest(BranchLayoutFileParser.class)
public class BranchLayoutFileParserTest {

  private BranchLayoutFileParser getBranchLayoutFileParserForLines(List<String> linesToReturn) throws Exception {
    BranchLayoutFileParser parser = PowerMockito.spy(new BranchLayoutFileParser(Path.of("")));
    doReturn(linesToReturn).when(parser, "getFileLines");
    return parser;
  }

  @Test
  public void parse_givenCorrectFile_parses() throws Exception {
    // given
    List<String> linesToReturn = List.of("A", " B", "C");
    BranchLayoutFileParser parser = getBranchLayoutFileParserForLines(linesToReturn);

    // when
    BranchLayout branchLayout = parser.parse();

    // then
    Assert.assertTrue(branchLayout.findEntryByName("A").isPresent());
    Assert.assertTrue(branchLayout.findEntryByName("B").isPresent());
    Assert.assertTrue(branchLayout.findEntryByName("C").isPresent());
  }

  @Test
  public void parse_givenCorrectFileWithRootsOnly_parses() throws Exception {
    // given
    List<String> linesToReturn = List.of("A", "B");
    BranchLayoutFileParser parser = getBranchLayoutFileParserForLines(linesToReturn);

    // when
    BranchLayout branchLayout = parser.parse();

    // then
    Assert.assertTrue(branchLayout.findEntryByName("A").isPresent());
    Assert.assertTrue(branchLayout.findEntryByName("B").isPresent());
  }

  @Test
  public void parse_givenEmptyFile_parses() throws Exception {
    // given
    List<String> linesToReturn = List.empty();
    BranchLayoutFileParser parser = getBranchLayoutFileParserForLines(linesToReturn);

    // when
    BranchLayout branchLayout = parser.parse();

    // then no exception thrown
  }

  @Test(expected = BranchLayoutException.class)
  public void parse_givenFileWithIndentedFirstEntry_throwsException() throws Exception {
    // given
    List<String> linesToReturn = List.of(" A", " B");
    BranchLayoutFileParser parser = getBranchLayoutFileParserForLines(linesToReturn);

    // when
    BranchLayout branchLayout = parser.parse();

    // then exception is thrown
  }

  @Test(expected = BranchLayoutException.class)
  public void parse_givenFileWithIdentWidthNotAMultiplicityOfLevelWidth_throwsException() throws Exception {
    // given
    List<String> linesToReturn = List.of("A", "   B", " C");
    BranchLayoutFileParser parser = getBranchLayoutFileParserForLines(linesToReturn);

    // when
    BranchLayout branchLayout = parser.parse();

    // then exception is thrown
  }

  @Test(expected = BranchLayoutException.class)
  public void parse_givenFileWithSubentryIdentGreaterThanOneToParent_throwsException() throws Exception {
    // given
    List<String> linesToReturn = List.of("A", "  B", "      C");
    BranchLayoutFileParser parser = getBranchLayoutFileParserForLines(linesToReturn);

    // when
    BranchLayout branchLayout = parser.parse();

    // then exception is thrown
  }
}
