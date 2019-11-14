package com.virtuslab;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.eclipse.jgit.lib.PersonIdent;


@EqualsAndHashCode
public class PersonIdentity implements IPersonIdentity {
    private PersonIdent jgitPerson;

    public PersonIdentity(PersonIdent person) {
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
