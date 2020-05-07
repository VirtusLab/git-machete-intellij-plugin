package com.virtuslab.gitmachete.backend.api;

import java.time.Instant;

public interface IGitMacheteCommit {
  String getMessage();

  String getHash();

  Instant getCommitTime();
}
