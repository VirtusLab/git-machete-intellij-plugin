package com.virtuslab;

public interface IPersonIdentity {
    String getName() throws GitException;
    String getEmail() throws GitException;
}
