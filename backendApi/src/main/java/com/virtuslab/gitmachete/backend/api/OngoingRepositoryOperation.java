package com.virtuslab.gitmachete.backend.api;

public enum OngoingRepositoryOperation {
  NO_OPERATION, CHERRY_PICKING, MERGING, REBASING, REVERTING, APPLYING, BISECTING
}
