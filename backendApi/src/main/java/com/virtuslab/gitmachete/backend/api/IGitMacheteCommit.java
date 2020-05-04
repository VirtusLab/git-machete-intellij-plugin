package com.virtuslab.gitmachete.backend.api;

import java.util.Date;

public interface IGitMacheteCommit {
  String getMessage();

  String getHash();

  Date getCommitDate();
}
