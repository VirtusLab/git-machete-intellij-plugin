package com.virtuslab.gitcore.gitcoreapi;

public interface IPersonIdentity {
    String getName() throws GitException;
    String getEmail() throws GitException;
}
