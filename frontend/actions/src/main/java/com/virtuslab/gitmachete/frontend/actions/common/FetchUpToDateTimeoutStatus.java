package com.virtuslab.gitmachete.frontend.actions.common;

import git4idea.repo.GitRepository;

public final class FetchUpToDateTimeoutStatus {
  private FetchUpToDateTimeoutStatus() {}

  public static final long FETCH_ALL_UP_TO_DATE_TIMEOUT_MILLIS = 60 * 1000;
  public static final String FETCH_ALL_UP_TO_DATE_TIMEOUT_AS_STRING = "a minute";

  @SuppressWarnings("ConstantName")
  private static final java.util.concurrent.ConcurrentMap<String, Long> lastFetchTimeMillisByRepoKey = new java.util.concurrent.ConcurrentHashMap<>();

  public static boolean isUpToDate(GitRepository gitRepository) {
    long lftm = lastFetchTimeMillisByRepoKey.getOrDefault(getRepoKey(gitRepository), 0L);
    return System.currentTimeMillis() < lftm + FETCH_ALL_UP_TO_DATE_TIMEOUT_MILLIS;
  }

  public static void update(GitRepository gitRepository) {
    lastFetchTimeMillisByRepoKey.put(getRepoKey(gitRepository), System.currentTimeMillis());
  }

  // We key by the absolute repo root path so that two repos with the same final
  // directory name (e.g. `~/work/projA/api/.git` and `~/work/projB/api/.git`),
  // including across multi-root projects and across separately-opened projects
  // sharing this app-level cache, don't end up sharing state.
  private static String getRepoKey(GitRepository gitRepository) {
    return gitRepository.getRoot().getPath();
  }

  // Visible for tests.
  static void clear() {
    lastFetchTimeMillisByRepoKey.clear();
  }
}
