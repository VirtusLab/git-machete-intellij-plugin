package com.virtuslab.gitmachete.graph;

import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Ahead;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Behind;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Diverged;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Untracked;

import java.util.Map;

public final class SyncToOriginStatusDescriptionGenerator {
	private SyncToOriginStatusDescriptionGenerator() {
	}

	private static final Map<Integer, String> descriptions = Map.of(Untracked.getId(), "untracked", Ahead.getId(),
			"ahead of origin", Behind.getId(), "behind origin", Diverged.getId(), "diverged from origin");

	public static String getDescription(int statusId) {
		return descriptions.getOrDefault(statusId, "sync to origin unknown");
	}
}
