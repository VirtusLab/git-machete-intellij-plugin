package com.virtuslab.gitcore.impl.jgit;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.eclipse.jgit.lib.PersonIdent;

import com.virtuslab.gitcore.api.IGitCorePersonIdentity;

@EqualsAndHashCode
@Getter
public class GitCorePersonIdentity implements IGitCorePersonIdentity {
  private final String name;
  private final String email;

  public GitCorePersonIdentity(PersonIdent person) {
    if (person == null) {
      throw new NullPointerException("Person passed to PersonIdentity constructor cannot be null");
    }
    this.name = person.getName();
    this.email = person.getEmailAddress();
  }
}
