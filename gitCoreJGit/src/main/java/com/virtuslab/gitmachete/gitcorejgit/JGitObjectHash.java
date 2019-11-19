package com.virtuslab.gitmachete.gitcorejgit;

import com.virtuslab.gitmachete.gitcore.ICommitHash;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@EqualsAndHashCode
public class JGitObjectHash implements ICommitHash {
    @Getter
    private String hashString;
}
