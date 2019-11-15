package com.virtuslab.gitmachete.gitcorejgit;

import com.virtuslab.gitmachete.gitcore.IPersonIdentity;
import lombok.EqualsAndHashCode;
import org.eclipse.jgit.lib.PersonIdent;


@EqualsAndHashCode
public class JGitPersonIdentity implements IPersonIdentity {
    private PersonIdent jgitPerson;

    public JGitPersonIdentity(PersonIdent person) {
        if(person == null)
            throw new NullPointerException("Person passed to PersonIdentity constructor cannot be null");
        this.jgitPerson = person;
    }

    @Override
    public String getName() {
        return jgitPerson.getName();
    }

    @Override
    public String getEmail() {
        return jgitPerson.getEmailAddress();
    }
}
