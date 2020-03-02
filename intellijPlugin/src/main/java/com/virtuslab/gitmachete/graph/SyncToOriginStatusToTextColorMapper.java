package com.virtuslab.gitmachete.graph;

import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Ahead;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Behind;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Diverged;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Untracked;
import static com.virtuslab.gitmachete.graph.ColorDefinitions.ORANGE;
import static com.virtuslab.gitmachete.graph.ColorDefinitions.RED;

import java.util.Map;

import com.intellij.ui.JBColor;

public final class SyncToOriginStatusToTextColorMapper {
	private SyncToOriginStatusToTextColorMapper() {
	}

	private static final Map<Integer, JBColor> colors = Map.of(Untracked.getId(), ORANGE, Ahead.getId(), RED,
			Behind.getId(), RED, Diverged.getId(), RED);

	public static JBColor getColor(int statusId) {
		return colors.getOrDefault(statusId, JBColor.GRAY);
	}
}
