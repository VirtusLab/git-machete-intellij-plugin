package com.virtuslab.gitmachete.frontend.actions.common;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation;

public final class SyncToRemoteStatusDescriptionProvider {

  private SyncToRemoteStatusDescriptionProvider() {}

  public static String syncToRemoteStatusRelationToReadableBranchDescription(Relation relation) {
    var desc = Match(relation).of(
        Case($(Relation.AheadOfRemote), "ahead of its remote"),
        Case($(Relation.BehindRemote), "behind its remote"),
        Case($(Relation.DivergedFromAndNewerThanRemote), "diverged from (& newer than) its remote"),
        Case($(Relation.DivergedFromAndOlderThanRemote), "diverged from (& older than) its remote"),
        Case($(Relation.InSyncToRemote), "in sync to its remote"),
        Case($(Relation.Untracked), "untracked"),
        Case($(), "in unknown status '${relation.toString()}' to its remote"));
    return "the branch is ${desc}";
  }
}
