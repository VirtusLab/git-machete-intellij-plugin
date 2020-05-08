package com.virtuslab.gitmachete.frontend.graph.api.labeling;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Ahead;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Behind;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedAndOlderThanRemote;
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
        // To avoid clutter we omit `& newer than` part in status label, coz this is default situation
        Case($(DivergedAndNewerThanRemote), "diverged from " + remoteName),
        Case($(DivergedAndOlderThanRemote), "diverged from & older than " + remoteName),
        Case($(), "synchronization to " + remoteName + " is unknown"));
  }
}
