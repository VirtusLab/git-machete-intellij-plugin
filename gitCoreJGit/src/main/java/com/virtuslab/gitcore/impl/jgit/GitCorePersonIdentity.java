package com.virtuslab.gitcore.impl.jgit;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.lib.PersonIdent;

import com.virtuslab.gitcore.api.IGitCorePersonIdentity;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GitCorePersonIdentity implements IGitCorePersonIdentity {
  private final String name;
  private final String email;

  public static GitCorePersonIdentity of(PersonIdent person) {
    return new GitCorePersonIdentity(person.getName(), person.getEmailAddress());
  }
}
