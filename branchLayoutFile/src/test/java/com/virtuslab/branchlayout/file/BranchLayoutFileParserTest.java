package com.virtuslab.branchlayout.file;

import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;

import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.vavr.collection.List;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.file.impl.BranchLayout;

@RunWith(PowerMockRunner.class)
@PrepareForTest(BranchLayoutFileParser.class)
public class BranchLayoutFileParserTest {

  private BranchLayoutFileParser getBranchLayoutFileParserForLines(List<String> linesToReturn) throws Exception {
    BranchLayoutFileParser parser = spy(new BranchLayoutFileParser(Path.of("")));
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
    Assert.assertTrue(branchLayout.findEntryByName("A").isDefined());
    Assert.assertTrue(branchLayout.findEntryByName("B").isDefined());
    Assert.assertTrue(branchLayout.findEntryByName("C").isDefined());
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
