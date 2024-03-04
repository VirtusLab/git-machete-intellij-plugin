package com.virtuslab.gitcore.impl.jgit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import lombok.val;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;

public class GitCoreCommitUnitTest {

  private static final String rawCommitData = """
      tree b8518260a35f740dbaa8161feda53017ab8c8be4
      parent e3be034fdef163e288f8219664f0df447bfe0ec3
      author foo <foo@example.com> 1664994622 +0200
      author foo <foo@example.com> 1664994622 +0200

      First line of subject
      - another line of subject
      - moar lines of subject

      First line of description
      More lines of description
      """;

  @Test
  public void shouldOnlyIncludeFirstLineOfSubjectInShortMessage() {
    val revCommit = RevCommit.parse(rawCommitData.getBytes(StandardCharsets.UTF_8));
    val commit = new GitCoreCommit(revCommit);

    assertEquals("First line of subject", commit.getShortMessage());
  }
}
