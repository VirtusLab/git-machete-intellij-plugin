package com.virtuslab.gitmachete.gitmacheteapi;

public enum SyncToParentStatus {
	Merged(0), InSyncButForkPointOff(1), OutOfSync(2), InSync(3);

	private final int id;

	SyncToParentStatus(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
}
