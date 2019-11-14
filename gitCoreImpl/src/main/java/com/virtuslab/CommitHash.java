package com.virtuslab;


import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@EqualsAndHashCode
public class CommitHash implements ICommitHash {
    @Getter
    private String hash;
}
