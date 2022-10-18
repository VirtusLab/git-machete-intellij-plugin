package com.virtuslab.gitcore.impl.jgit;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import lombok.val;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

public class GitCoreCommitTest {

  private static final String rawCommitData = "tree b8518260a35f740dbaa8161feda53017ab8c8be4\n" +
      "parent e3be034fdef163e288f8219664f0df447bfe0ec3\n" +
      "author foo <foo@example.com> 1664994622 +0200\n" +
      "author foo <foo@example.com> 1664994622 +0200\n" +
      "\n" +
      "First line of subject\n" +
      "- another line of subject\n" +
      "- moar lines of subject\n" +
      "\n" +
      "First line of description\n" +
      "More lines of description";

  @Test
  public void shouldOnlyIncludeFirstLineOfSubjectInShortMessage() {
    val revCommit = RevCommit.parse(rawCommitData.getBytes(StandardCharsets.UTF_8));
    val commit = new GitCoreCommit(revCommit);

    assertEquals("First line of subject", commit.getShortMessage());
  }
}
