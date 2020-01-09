package com.virtuslab.gitmachete.file;

import lombok.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Getter
public class GitMacheteFileBranchEntry {
    @NonNull
    private String name;
    @NonNull
    private Optional<GitMacheteFileBranchEntry> upstream;
    private List<GitMacheteFileBranchEntry> subbranches = new LinkedList<>();
    @NonNull
    private Optional<String> customAnnotation;

    void addSubbranch(GitMacheteFileBranchEntry subbranch) {
        subbranches.add(subbranch);
    }
}
