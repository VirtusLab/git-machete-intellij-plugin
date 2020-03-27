package com.virtuslab.gitcore.impl.jgit;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.jgit.lib.PersonIdent;

import com.virtuslab.gitcore.api.IGitCorePersonIdentity;

@EqualsAndHashCode
@Getter
@ToString
public class GitCorePersonIdentity implements IGitCorePersonIdentity {
  private final String name;
  private final String email;

  public GitCorePersonIdentity(PersonIdent person) {
    this.name = person.getName();
    this.email = person.getEmailAddress();
  }
}
