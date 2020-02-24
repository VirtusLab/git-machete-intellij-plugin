package com.virtuslab.gitcore.impl.jgit;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IAncestorityChecker;
import java.io.IOException;
import lombok.Data;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

@Data
class AncestorityChecker implements IAncestorityChecker {
  private final Repository jgitRepo;

  @Override
  public boolean check(String commitHash, String parentCommitHash) throws GitCoreException {
    RevWalk walk = new RevWalk(jgitRepo);
    walk.sort(RevSort.TOPO);
    try {
      ObjectId objectId = jgitRepo.resolve(commitHash);
      walk.markStart(walk.parseCommit(jgitRepo.resolve(parentCommitHash)));

      for (var c : walk) {
        if (c.getId().equals(objectId)) {
          return true;
        }
      }
    } catch (IOException e) {
      throw new GitCoreException(e);
    }

    return false;
  }
}
