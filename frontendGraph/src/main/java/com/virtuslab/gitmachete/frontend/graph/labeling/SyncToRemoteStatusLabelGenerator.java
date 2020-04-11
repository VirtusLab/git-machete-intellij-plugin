package com.virtuslab.gitmachete.frontend.graph.labeling;

import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.Ahead;
import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.Behind;
import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.Diverged;
import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.Untracked;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus;

public final class SyncToRemoteStatusLabelGenerator {
  private SyncToRemoteStatusLabelGenerator() {}

  public static String getLabel(ISyncToRemoteStatus.Relation relation, String remoteName) {
    return Match(relation).of(
        Case($(Untracked), "untracked"),
        Case($(Ahead), "ahead of " + remoteName),
        Case($(Behind), "behind " + remoteName),
        Case($(Diverged), "diverged from " + remoteName),
        Case($(), "synchronization to " + remoteName + " is unknown"));
  }
}
