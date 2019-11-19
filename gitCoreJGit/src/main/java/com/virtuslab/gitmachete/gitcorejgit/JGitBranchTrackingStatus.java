package com.virtuslab.gitmachete.gitcorejgit;

import com.virtuslab.gitmachete.gitcore.IBranchTrackingStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.MessageFormat;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JGitBranchTrackingStatus implements IBranchTrackingStatus {
    @Getter
    private int ahead;
    @Getter
    private int behind;

    static JGitBranchTrackingStatus build(int ahead, int behind) {
        return new JGitBranchTrackingStatus(ahead, behind);
    }

    @Override
    public String toString() {
        return MessageFormat.format( "Ahead: {0}; Behind: {1}", ahead, behind);
    }
}
