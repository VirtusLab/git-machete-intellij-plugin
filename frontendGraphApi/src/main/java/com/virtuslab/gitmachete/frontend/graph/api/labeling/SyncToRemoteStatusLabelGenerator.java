package com.virtuslab.gitmachete.frontend.graph.api.labeling;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote;
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
        Case($(AheadOfRemote), "ahead of " + remoteName),
        Case($(BehindRemote), "behind " + remoteName),
        // To avoid clutter we omit `& newer than` part in status label, coz this is default situation
        Case($(DivergedFromAndNewerThanRemote), "diverged from " + remoteName),
        Case($(DivergedFromAndOlderThanRemote), "diverged from & older than " + remoteName),
        Case($(), "synchronization to " + remoteName + " is unknown"));
  }
}
