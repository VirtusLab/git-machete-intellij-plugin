package com.virtuslab.gitmachete.frontend.graph.labeling;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Ahead;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Behind;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Diverged;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Untracked;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

public final class SyncToRemoteStatusLabelGenerator {
  private SyncToRemoteStatusLabelGenerator() {}

  public static String getLabel(SyncToRemoteStatus.Relation relation, String remoteName) {
    return Match(relation).of(
        Case($(Untracked), "untracked"),
        Case($(Ahead), "ahead of " + remoteName),
        Case($(Behind), "behind " + remoteName),
        Case($(Diverged), "diverged from " + remoteName),
        Case($(), "synchronization to " + remoteName + " is unknown"));
  }
}
