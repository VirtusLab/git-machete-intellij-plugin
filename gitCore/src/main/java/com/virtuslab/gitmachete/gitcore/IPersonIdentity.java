package com.virtuslab.gitmachete.gitcore;

public interface IPersonIdentity {
    String getName() throws GitException;
    String getEmail() throws GitException;
}
