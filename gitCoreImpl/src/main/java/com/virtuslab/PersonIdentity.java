package com.virtuslab;

import org.eclipse.jgit.lib.PersonIdent;

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

    @Override
    public boolean equals(Object o) {
        if(o == this)
            return true;

        if(!(o instanceof PersonIdentity))
            return false;

        return jgitPerson.equals(((PersonIdentity) o).jgitPerson);
    }
}
